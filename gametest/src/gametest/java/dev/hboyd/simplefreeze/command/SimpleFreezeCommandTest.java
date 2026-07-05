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

package dev.hboyd.simplefreeze.command;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientLevelContext;
import net.fabricmc.fabric.impl.client.gametest.context.TestClientLevelContextImpl;
import net.fabricmc.fabric.impl.client.gametest.context.TestDedicatedServerContextImpl;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import net.fabricmc.fabric.impl.client.gametest.util.ClientGameTestImpl;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

@SuppressWarnings("UnstableApiUsage")
public class SimpleFreezeCommandTest implements FabricClientGameTest {
    @Override
    public void runTest(final ClientGameTestContext context) {
        final TestDedicatedServerContextImpl testDedicatedServerContext = new TestDedicatedServerContextImpl(context, null);
        ThreadingImpl.checkOnGametestThread("connect");

        context.runOnClient(client -> {
            final var serverInfo = new ServerData("localhost", "localhost:25565", ServerData.Type.OTHER);
            ConnectScreen.startConnecting(client.screen,
                    client,
                    ServerAddress.parseString("localhost:25565"),
                    serverInfo,
                    false,
                    null);
        });

        ClientGameTestImpl.waitForWorldLoad(context);

        TestClientLevelContext clientLevel = new TestClientLevelContextImpl(context);
        context.waitTicks(500);
    }
}
