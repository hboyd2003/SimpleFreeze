/*
 * Simple Freeze
 * Copyright (c) 2026 Harrison Boyd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.hboyd.simplefreeze;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import dev.hboyd.simplefreeze.database.*;
import dev.hboyd.simplefreeze.util.PacketEventsUtil;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.event.executor.EventExecutorFactory;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;
import org.seasar.doma.jdbc.Config;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class FreezeManager implements IFreezeManager, Listener , PacketListener {
    private static final String FROZEN_SCOREBOARD_TAG = "frozen";
    private static final Title.Times FROZEN_TITLE_TIMES = Title.Times.times(Duration.ZERO, Ticks.duration(20), Duration.ZERO);
    private static final int BLOCK_DISPLAY_ENTITY_ID = Integer.MAX_VALUE;

    /**
     * List of all cancellable events that are excluded from the standard handling or not handled at all
     */
    private static final List<Class<? extends Event>> CANCELLABLE_EVENT_EXCEPTIONS = List.of(
            PlayerKickEvent.class, // Players should still be kickable
            PlayerTrackEntityEvent.class, // Players should still be able to track entities
            PlayerStartSpectatingEntityEvent.class, // Players can't be frozen in spectator mode
            EntityResurrectEvent.class,  // Players can't take damage or die
            EntityTargetEvent.class, // Special handling
            EntitySpawnEvent.class, // Player cannot be mounted at moment of spawn
            EntityDismountEvent.class, // Player's must be able to dismount an entity of disconnect
            EntityMountEvent.class, // Players must be able to re-mount their entity on login
            ArrowBodyCountChangeEvent.class, // Arrows cannot affect players
            VehicleCreateEvent.class, // Players cannot be mounted at moment of spawn
            VehicleExitEvent.class, // Players must be able to dismount on disconnect
            VehicleEnterEvent.class, // Players need to be able to mount a vehicle on re-join/respawn
            // Handled by packet event listener (triggered by client only)
            PlayerJumpEvent.class,
            PlayerItemHeldEvent.class,
            PlayerPickItemEvent.class,
            PlayerRecipeBookClickEvent.class,
            PlayerSwapHandItemsEvent.class,
            PlayerToggleFlightEvent.class,
            PlayerToggleSneakEvent.class,
            PlayerToggleSprintEvent.class
    );

    private final FreezeEntryDao freezeEntryDao;
    private final PreFreezeStateDao preFreezeStateDao;
    private final boolean alwaysDisconnectWithEntity;

    FreezeManager(Config databaseConfig, boolean alwaysDisconnectWithEntity) {
        this.freezeEntryDao = new FreezeEntryDaoImpl(databaseConfig);
        this.preFreezeStateDao = new PreFreezeStateDaoImpl(databaseConfig);
        this.alwaysDisconnectWithEntity = alwaysDisconnectWithEntity;

        this.freezeEntryDao.createIfNotExists();
        this.preFreezeStateDao.createIfNotExists();

        Bukkit.getServer().getAsyncScheduler().runAtFixedRate(SimpleFreeze.INSTANCE,
                this::tick,
                1,
                50,
                TimeUnit.MILLISECONDS);
        try {
            registerEvents(findCancellableEvents(PlayerEvent.class), EventPriority.HIGHEST, this.getClass().getDeclaredMethod("handleCancellablePlayerEvent", PlayerEvent.class));
            registerEvents(findCancellableEvents(EntityEvent.class), EventPriority.HIGHEST, this.getClass().getDeclaredMethod("handleCancellableEntityEvent", EntityEvent.class));
            registerEvents(findCancellableEvents(VehicleEvent.class), EventPriority.HIGHEST, this.getClass().getDeclaredMethod("handleCancellableVehicleEvent", VehicleEvent.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    public boolean isPlayerFrozen(OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");

        Player player = offlinePlayer.getPlayer();
        if (player != null) return player.getScoreboardTags().contains(FROZEN_SCOREBOARD_TAG);

        return this.preFreezeStateDao.exists(offlinePlayer.getUniqueId());
    }

    @Override
    public @Unmodifiable LinkedHashSet<Key> getFreezeEntries(OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");

        return new LinkedHashSet<>(this.freezeEntryDao.getKeys(offlinePlayer.getUniqueId()));
    }

    @Override
    public @Unmodifiable Map<OfflinePlayer, LinkedHashSet<Key>> getFreezeEntryMap() {
        Map<UUID, LinkedHashSet<Key>> freezeEntryMap = new HashMap<>();
        for (FreezeEntry freezeEntry : this.freezeEntryDao.get()) {
            LinkedHashSet<Key> keySet = freezeEntryMap.getOrDefault(freezeEntry.playerUuid(), new LinkedHashSet<>());
            keySet.add(freezeEntry.entryKey());
            freezeEntryMap.put(freezeEntry.playerUuid(), keySet);
        }

        return freezeEntryMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> Bukkit.getOfflinePlayer(entry.getKey()),
                        Map.Entry::getValue));
    }

    @Override
    public @Unmodifiable Set<OfflinePlayer> frozenPlayers() {
        return this.freezeEntryDao.getUuids().stream()
                .map(Bukkit::getOfflinePlayer)
                .collect(Collectors.toSet());
    }

    @Override
    public @Unmodifiable Set<OfflinePlayer> frozenPlayers(Key entryKey) {
        Objects.requireNonNull(entryKey, "entryKey");

        return this.freezeEntryDao.getUuids(entryKey).stream()
                .map(Bukkit::getOfflinePlayer)
                .collect(Collectors.toSet());
    }

    @Override
    public void addFreezeEntry(OfflinePlayer player, Key entryKey) {
        addFreezeEntry(player, entryKey, null);
    }

    @Override
    public void addFreezeEntry(OfflinePlayer offlinePlayer, Key entryKey, @Nullable Component title) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        Objects.requireNonNull(entryKey, "entryKey");

        Player player = offlinePlayer.getPlayer();
        if (player != null && !player.isDead()  && !this.freezeEntryDao.exists(player.getUniqueId()))
            setPlayerFreezeState(player);

        this.freezeEntryDao.insert(new FreezeEntry(offlinePlayer.getUniqueId(), entryKey, title));

        SimpleFreeze.LOGGER.info("Froze {}", offlinePlayer.getName());
    }

    @Override
    public boolean removeFreezeEntry(OfflinePlayer offlinePlayer, Key entryKey) throws IllegalStateException {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        Objects.requireNonNull(entryKey, "entryKey");

        if (this.freezeEntryDao.delete(offlinePlayer.getUniqueId(), entryKey) >= 1) {
            Player player = offlinePlayer.getPlayer();
            if (player != null && !this.freezeEntryDao.exists(player.getUniqueId()))
                restore(player);

            return true;
        }

        return false;
    }

    @Override
    public boolean forceUnfreezePlayer(OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");

        this.freezeEntryDao.deleteAll(offlinePlayer.getUniqueId());

        Optional<PreFreezeState> preFreezeState = this.preFreezeStateDao.get(offlinePlayer.getUniqueId());
        boolean hadPreFreezeState = preFreezeState.isPresent();

        Player player = offlinePlayer.getPlayer();
        if (player != null) {
            preFreezeState.orElseGet(() -> PreFreezeState
                            .defaultOf(player))
                    .restoreTo(player);
            this.preFreezeStateDao.delete(player.getUniqueId());
            player.removeScoreboardTag(FROZEN_SCOREBOARD_TAG);
            this.removeVirtualSpectator(player);
            player.clearTitle();
            player.sendActionBar(Component.empty());
        }
        return hadPreFreezeState;
    }

    private void tick(ScheduledTask task) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.isPlayerFrozen(player)) continue;

            player.showTitle(Title.title(
                    this.freezeEntryDao.getLastTitle(player.getUniqueId()).orElseGet(Component::empty),
                    Component.translatable("simplefreeze.ui.frozen.subtitle"),
                    FROZEN_TITLE_TIMES));

            player.sendActionBar(Component.translatable("simplefreeze.ui.frozen.actionbar"));

            reaffirmFreezeState(player);
            player.resetIdleDuration();
        }
    }

    //region Events
    @Override
    @ApiStatus.Internal
    public void onPacketReceive(PacketReceiveEvent event) {
        final Player player = event.getPlayer();
        if (event.isCancelled()
                || player == null
                || !this.isPlayerFrozen(player))
            return;

        boolean cancelled = switch (event.getPacketType()) {
            case PacketType.Play.Client.PLAYER_INPUT,
                 PacketType.Play.Client.PLAYER_ROTATION,
                 PacketType.Play.Client.PLAYER_POSITION,
                 PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                 PacketType.Play.Client.VEHICLE_MOVE,
                 PacketType.Play.Client.INTERACT_ENTITY -> true;
            case PacketType.Play.Client.PICK_ITEM,
                 PacketType.Play.Client.PICK_ITEM_FROM_BLOCK,
                 PacketType.Play.Client.PICK_ITEM_FROM_ENTITY,
                 PacketType.Play.Client.SLOT_STATE_CHANGE -> {
                player.updateInventory();

                yield true;
            }
            case PacketType.Play.Client.HELD_ITEM_CHANGE -> {
                WrapperPlayServerHeldItemChange heldItemChangePacket = new WrapperPlayServerHeldItemChange(player.getInventory().getHeldItemSlot());
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, heldItemChangePacket);
                player.updateInventory();

                yield true;
            }
            case PacketType.Play.Client.CLICK_WINDOW,
                 PacketType.Play.Client.CLICK_WINDOW_BUTTON,
                 PacketType.Play.Client.CREATIVE_INVENTORY_ACTION -> {
                WrapperPlayServerCloseWindow closeWindowPacket = new WrapperPlayServerCloseWindow();
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, closeWindowPacket);
                player.updateInventory();

                yield true;
            }
            default -> false;
        };

        event.setCancelled(cancelled);
    }

    private void registerEvents(Collection<Class<? extends Event>> events, EventPriority priority, Method method) {
        for (Class<? extends Event> event : events) {
            Bukkit.getPluginManager().registerEvent(event,
                    this,
                    priority,
                    EventExecutorFactory.create(method, event), // Internal factory needs to be used to bypass checks
                    SimpleFreeze.INSTANCE);
        }
    }

    @ApiStatus.Internal
    public void handleCancellablePlayerEvent(PlayerEvent event) {
        if (this.isEntityFrozen(event.getPlayer()))
            ((Cancellable) event).setCancelled(true);
    }

    @ApiStatus.Internal
    public void handleCancellableEntityEvent(EntityEvent event) {
        if (this.isEntityFrozen(event.getEntity()))
            ((Cancellable) event).setCancelled(true);
    }

    @ApiStatus.Internal
    public void handleCancellableVehicleEvent(VehicleEvent event) {
        if (this.isEntityFrozen(event.getVehicle()))
            ((Cancellable) event).setCancelled(true);
    }

    private List<Class<? extends Event>> findCancellableEvents(Class<? extends Event> eventSuperClass) {
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().enableMethodInfo().acceptClasses().scan()) {
            @SuppressWarnings("unchecked") List<Class<? extends Event>> cancellableEvents = scanResult
                    .getClassesImplementing(Cancellable.class)
                    .getStandardClasses()
                    .filter(classInfo -> classInfo.hasDeclaredMethod("getHandlerList"))
                    .filter(classInfo -> classInfo.extendsSuperclass(eventSuperClass))
                    .stream()
                    .map(classInfo -> (Class<? extends Event>) classInfo.loadClass())
                    .filter(eventClass -> !CANCELLABLE_EVENT_EXCEPTIONS.contains(eventClass))
                    .collect(Collectors.toList());

            ComponentBuilder<TextComponent, TextComponent.Builder> componentBuilder = Component.text().appendNewline().append(Component.text("Cancellable " + eventSuperClass.getName() + " Events: "));
            for (Class<?> cancellableEvent : cancellableEvents) {
                componentBuilder.appendNewline().append(Component.text(cancellableEvent.getName()));
            }
            SimpleFreeze.LOGGER.info(componentBuilder.build());
            return cancellableEvents;
        }
    }

    @EventHandler
    private void onTargetEvent(EntityTargetEvent event) {
        if (event.getTarget() == null) return;

        if (this.isEntityFrozen(event.getTarget()) || this.isEntityFrozen(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler
    private void onEnchantItem(EnchantItemEvent event) {
        if (this.isPlayerFrozen(event.getEnchanter())) event.setCancelled(true);
    }

    @EventHandler
    private void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (this.isEntityFrozen(event.getEnchanter())) event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerJoinEvent(PlayerJoinEvent event) {
        // Sync freeze state if changed offline or reestablish virtual spectator if needed
        if (event.getPlayer().getScoreboardTags().contains(FROZEN_SCOREBOARD_TAG)) {
            if (!this.freezeEntryDao.exists(event.getPlayer().getUniqueId()))
                restore(event.getPlayer());
            else configureVirtualSpectator(event.getPlayer());
        } else if (this.freezeEntryDao.exists(event.getPlayer().getUniqueId()))
            setPlayerFreezeState(event.getPlayer());
    }

    @EventHandler
    private void onPlayerQuitEvent(PlayerQuitEvent event) {
        if (!this.isPlayerFrozen(event.getPlayer())) return;

        final Entity vehicle = event.getPlayer().getVehicle();
        if (vehicle == null) return;

        if (this.alwaysDisconnectWithEntity) {
            while (vehicle.getPassengers().size() > 1)
                vehicle.removePassenger(vehicle.getPassengers().getLast());
        }
    }

    @EventHandler
    private void onPlayerPostRespawn(PlayerPostRespawnEvent event) {
        // Sync freeze state if changed while dead
        if (event.getPlayer().getScoreboardTags().contains(FROZEN_SCOREBOARD_TAG)) {
            if (!this.freezeEntryDao.exists(event.getPlayer().getUniqueId()))
                restore(event.getPlayer());
        } else if (this.freezeEntryDao.exists(event.getPlayer().getUniqueId()))
            setPlayerFreezeState(event.getPlayer());
    }
    //endregion

    private void restore(Player player) {
        // Restore vehicle
        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle.getPassengers().getFirst() == player) {
            vehicle.getScoreboardTags().remove(FROZEN_SCOREBOARD_TAG);

            Optional<PreFreezeState> preFreezeState = this.preFreezeStateDao.get(vehicle.getUniqueId());
            if (preFreezeState.isPresent()) preFreezeState.get().restoreTo(vehicle);
            else {
                SimpleFreeze.LOGGER.warn("Failed to find pre-freeze state for entity {} ({}). Entity will be set to a default state", vehicle.getType().getKey(), vehicle.getUniqueId());
                PreFreezeState.defaultOf(player).restoreTo(player);
            }
            this.preFreezeStateDao.delete(vehicle.getUniqueId());

            final int[] vehiclePassengers = player.getVehicle().getPassengers().stream()
                    .map(Entity::getEntityId)
                    .mapToInt(i -> i)
                    .toArray();
            WrapperPlayServerSetPassengers playerSetPassengersPacket = new WrapperPlayServerSetPassengers(player.getVehicle().getEntityId(), vehiclePassengers);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, playerSetPassengersPacket);
        }

        // Restore player
        player.getScoreboardTags().remove(FROZEN_SCOREBOARD_TAG);

        player.clearTitle();
        player.sendActionBar(Component.empty());

        removeVirtualSpectator(player);
        Optional<PreFreezeState> preFreezeState = this.preFreezeStateDao.get(player.getUniqueId());
        if (preFreezeState.isPresent()) preFreezeState.get().restoreTo(player);
        else {
            SimpleFreeze.LOGGER.warn("Failed to find pre-freeze state for player {} ({}). Player will be set to a default state", player.getName(), player.getUniqueId());
            PreFreezeState.defaultOf(player).restoreTo(player);
        }
        this.preFreezeStateDao.delete(player.getUniqueId());

        // Restore passengers
        ArrayList<Entity> playerPassengers = new ArrayList<>(player.getPassengers());
        while (!playerPassengers.isEmpty()) {
            Entity passenger = playerPassengers.removeFirst();
            if (passenger instanceof Player) continue;

            passenger.removeScoreboardTag(FROZEN_SCOREBOARD_TAG);
            playerPassengers.addAll(player.getPassengers());
        }
    }

    private void reaffirmFreezeState(Player player) {
        // To the client, the player should never be in a vehicle, but if a client sent vehicle packets they would still be able to move. Thus, we send these to be safe.
        if (player.isInsideVehicle()) {
            final Location vehicleLocation = player.getVehicle().getLocation();

            final WrapperPlayServerVehicleMove serverVehicleMovePacket = new WrapperPlayServerVehicleMove(
                    PacketEventsUtil.toVector3d(vehicleLocation),
                    vehicleLocation.getYaw(),
                    vehicleLocation.getPitch());
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, serverVehicleMovePacket);

            player.getVehicle().getScheduler().run(SimpleFreeze.INSTANCE, _ -> {
                final WrapperPlayServerEntityVelocity vehicleVelocityPacket = new WrapperPlayServerEntityVelocity(player.getVehicle().getEntityId(), Vector3d.zero());
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, vehicleVelocityPacket);

                // Tell the client we aren't in a vehicle
                final WrapperPlayServerSetPassengers serverSetPassengers = new WrapperPlayServerSetPassengers(player.getVehicle().getEntityId(), new int[]{});
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, serverSetPassengers);
            }, null);
        }
    }

    private void setPlayerFreezeState(Player player) {
        ArrayList<Entity> passengers = new ArrayList<>(player.getPassengers());
        configureVirtualSpectator(player);
        while (!passengers.isEmpty()) {
            Entity passenger = passengers.removeFirst();
            if (passenger instanceof Player) continue;

            passenger.addScoreboardTag(FROZEN_SCOREBOARD_TAG);
            passengers.addAll(passenger.getPassengers());
        }

        // TODO: Determine if this is the correct size to scan for entities
        player.getNearbyEntities(Bukkit.getSimulationDistance(), Bukkit.getSimulationDistance(), Bukkit.getSimulationDistance()).stream()
                .filter(Mob.class::isInstance)
                .map(Mob.class::cast)
                .filter(mob -> mob.getTarget() == player)
                .forEach(mob -> mob.setTarget(null));

        if (player.isInsideVehicle() && player.getVehicle().getPassengers().getFirst() == player) {
            setEntityFreezeState(player.getVehicle());

            WrapperPlayServerSetPassengers playerSetPassengersPacket = new WrapperPlayServerSetPassengers(player.getVehicle().getEntityId(), new int[]{});
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, playerSetPassengersPacket);
        }

        player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
        setEntityFreezeState(player);
    }

    private void setEntityFreezeState(Entity entity) {
        this.preFreezeStateDao.insert(PreFreezeState.of(entity));

        entity.addScoreboardTag(FROZEN_SCOREBOARD_TAG);
        entity.setVelocity(new Vector());
        entity.setNoPhysics(true);
        entity.setSilent(true);
        entity.setGravity(false);

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setNextArrowRemoval(Integer.MAX_VALUE);
            livingEntity.setNextBeeStingerRemoval(Integer.MAX_VALUE);

            if (livingEntity instanceof Player player) {
                player.setFreezeTicks(player.getMaxFreezeTicks());
                player.lockFreezeTicks(true);
                player.setSleepingIgnored(true);
            }
        }
    }

    private boolean isEntityFrozen(Entity entity) {
        if (entity instanceof Player player) return isPlayerFrozen(player);

        return entity.getScoreboardTags().contains(FROZEN_SCOREBOARD_TAG);
    }

    /**
     * Configures the client with packets to spectate a block display entity preventing all player movement.
     *
     * @param player the player to configure
     */
    private void configureVirtualSpectator(Player player) {
        WrapperPlayServerSpawnEntity spawnEntityPacket = new WrapperPlayServerSpawnEntity(BLOCK_DISPLAY_ENTITY_ID,
                UUID.randomUUID(),
                EntityTypes.BLOCK_DISPLAY,
                PacketEventsUtil.toPacketEventsLocation(player.getEyeLocation()),
                player.getYaw(),
                0,
                null);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnEntityPacket);

        EntityData<Boolean> hasNoGravityEntityData = new EntityData<>(5, EntityDataTypes.BOOLEAN, true);
        WrapperPlayServerEntityMetadata entityMetadataPacket = new WrapperPlayServerEntityMetadata(BLOCK_DISPLAY_ENTITY_ID, List.of(hasNoGravityEntityData));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, entityMetadataPacket);

        WrapperPlayServerChangeGameState gameStatePacket = new WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, GameMode.SPECTATOR.getId());
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, gameStatePacket);

        // Tell the client to spectate the block display
        WrapperPlayServerCamera setCameraPacket = new WrapperPlayServerCamera(BLOCK_DISPLAY_ENTITY_ID);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, setCameraPacket);
    }

    private void removeVirtualSpectator(Player player) {
        WrapperPlayServerDestroyEntities destroyEntitiesPacket = new WrapperPlayServerDestroyEntities(BLOCK_DISPLAY_ENTITY_ID);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyEntitiesPacket);

        //
        WrapperPlayServerCamera camera = new WrapperPlayServerCamera(player.getEntityId());
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, camera);

        // Set to spectator
        WrapperPlayServerChangeGameState gameStatePacket = new WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, player.getGameMode().getValue());
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, gameStatePacket);
    }
}