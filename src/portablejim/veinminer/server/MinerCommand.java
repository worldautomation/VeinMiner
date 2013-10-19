/* This file is part of VeinMiner.
 *
 *    VeinMiner is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation, either version 3 of
 *     the License, or (at your option) any later version.
 *
 *    VeinMiner is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with VeinMiner.
 *    If not, see <http://www.gnu.org/licenses/>.
 */

package portablejim.veinminer.server;

import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import portablejim.veinminer.configuration.ConfigurationSettings;
import portablejim.veinminer.util.BlockID;

import java.util.List;

import static portablejim.veinminer.configuration.ConfigurationSettings.ToolType;

/**
 * Command so that clients can control VeinMiner settings for their player.
 */

public class MinerCommand extends CommandBase {
    public static final int COMMAND_MODE = 0;
    public static final int COMMAND_BLOCKLIST = 1;
    public static final int COMMAND_TOOLLIST = 2;
    public static final int COMMAND_BLOCKLIMIT = 3;
    public static final int COMMAND_RANGE = 4;
    public static final int COMMAND_PER_TICK = 5;
    public static final int COMMAND_HELP = 6;
    private static final String[] commands = new String[]{"mode", "blocklist", "toollist", "blocklimit", "radius", "per_tick", "help"};
    private static final String[] modes = new String[] {"disable", "auto", "sneak", "no_sneak"};

    @Override
    public String getCommandName() {
        return "veinminer";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender par1ICommandSender) {
        return par1ICommandSender instanceof EntityPlayerMP;
    }

    private ToolType commandTool(String[] commandString, String commandName) {
        if(commandString.length == 1) {
            throw new WrongUsageException("command.veinminer." + commandName);
        }

        ToolType tool;
        if("pickaxe".equals(commandString[1])) {
            tool = ToolType.PICKAXE;
        }
        else if("axe".equals(commandString[1])) {
            tool = ToolType.AXE;
        }
        else if("shovel".equals(commandString[1])) {
            tool = ToolType.SHOVEL;
        }
        else {
            throw new WrongUsageException("command.veinminer." + commandName);
        }

        return tool;
    }

    private void commandAction(String[] commandString, String commandName) {
        if (commandString.length < 3 || (!"add".equals(commandString[2]) && !"remove".equals(commandString[2]))) {
            throw new WrongUsageException("command.veinminer." + commandName + ".actionerror", commandString[1]);
        }
    }

    @Override
    public void processCommand(ICommandSender icommandsender, String[] astring) {
        EntityPlayerMP senderPlayer;
        if(icommandsender instanceof EntityPlayerMP) {
            senderPlayer = (EntityPlayerMP) icommandsender;
        }
        else {
            throw new CommandException("Non-players cannot use veinminer commands", icommandsender);
        }

        if(astring.length > 0) {
            if(astring[0].equals(commands[COMMAND_MODE])) {
                runCommandMode(senderPlayer, astring);
            }
            else if(astring[0].equals(commands[COMMAND_BLOCKLIST])) {
                runCommandBlocklist(senderPlayer, astring);
            }
            else if(astring[0].equals(commands[COMMAND_TOOLLIST])) {
                runCommandToollist(senderPlayer, astring);
            }
            else if(astring[0].equals(commands[COMMAND_BLOCKLIMIT])) {
                runCommandBlocklimit(senderPlayer, astring);
            }
            else if(astring[0].equals(commands[COMMAND_RANGE])) {
                runCommandRange(senderPlayer, astring);
            }
            else if(astring[0].equals(commands[COMMAND_PER_TICK])) {
                runCommandPerTick(senderPlayer, astring);
            }
            else if(astring[0].equals(commands[COMMAND_HELP])) {
                runCommandHelp(senderPlayer, astring);
            }
        }
        else
        {
            throw new WrongUsageException("command.veinminer");
        }
    }

    private void sendProperChatToPlayer(EntityPlayerMP player, String incomingMessage) {
        sendProperChatToPlayer(player, incomingMessage, new Object[]{});
    }

    private void sendProperChatToPlayer(EntityPlayerMP player, String incomingMessage, Object... params) {
        boolean playerNoClient = !MinerServer.instance.playerHasClient(player.getEntityName());
        String message = incomingMessage;
        if(playerNoClient) {
            message = LanguageRegistry.instance().getStringLocalization(incomingMessage);
            message = String.format(message, params);
        }
        player.addChatMessage(message);
    }

    private void showUsageError(EntityPlayerMP player, String errorKey) throws WrongUsageException {
        showUsageError(player, errorKey, new Object[]{});
    }

    private void showUsageError(EntityPlayerMP player, String errorKey, Object... params) {
        boolean playerNoClient = !MinerServer.instance.playerHasClient(player.getEntityName());
        String message = errorKey;
        if(playerNoClient) {
            message = LanguageRegistry.instance().getStringLocalization(errorKey);
            message = String.format(message, params);
        }
        throw new WrongUsageException(message);
    }

    private void runCommandMode(EntityPlayerMP senderPlayer, String[] astring) throws WrongUsageException {
        MinerServer minerServer = MinerServer.instance;
        String player = senderPlayer.getCommandSenderName();

        if(astring.length == 1) {
            showUsageError(senderPlayer, "command.veinminer.enable");
        }
        else if(astring[1].equals(modes[0])) {
            minerServer.setPlayerStatus(player, PlayerStatus.DISABLED);
            sendProperChatToPlayer(senderPlayer, "command.veinminer.set.disable");
        }
        else if(astring[1].equals(modes[1])) {
            if(minerServer.playerHasClient(player)) {
                minerServer.setPlayerStatus(player, PlayerStatus.INACTIVE);
            }
            else {
                minerServer.setPlayerStatus(player, PlayerStatus.DISABLED);
            }
            sendProperChatToPlayer(senderPlayer, "command.veinminer.set.auto");
        }
        else if(astring[1].equals(modes[2])) {
            minerServer.setPlayerStatus(player, PlayerStatus.SNEAK_ACTIVE);
            sendProperChatToPlayer(senderPlayer, "command.veinminer.set.sneak");
        }
        else if(astring[1].equals(modes[3])) {
            minerServer.setPlayerStatus(player, PlayerStatus.SNEAK_INACTIVE);
            sendProperChatToPlayer(senderPlayer, "command.veinminer.set.nosneak");
        }
    }

    private void runCommandBlocklist(EntityPlayerMP senderPlayer, String[] astring) {
        ConfigurationSettings configSettings = MinerServer.instance.getConfigurationSettings();

        ToolType tool = commandTool(astring, "blocklist");
        String toolString = astring[1];

        commandAction(astring, "blockList");
        String action = astring[2];

        if(astring.length < 4) {
            showUsageError(senderPlayer, "command.veinminer.blocklist.itemerror", toolString, action);
        }

        BlockID blockID = new BlockID(astring[3], ":", -1);
        if(blockID.id <= 0) {
            // String is not in proper format
            showUsageError(senderPlayer, "command.veinminer.blocklist.itemerror", toolString, action);
        }

        if("add".equals(action)) {
            configSettings.addBlockToWhitelist(tool, blockID);
            String message = LanguageRegistry.instance().getStringLocalization("command.veinminer.blocklist.add");
            sendProperChatToPlayer(senderPlayer, message, blockID.id, blockID.metadata, toolString);
        }
        else if("remove".equals(action)) {
            configSettings.removeBlockFromWhitelist(tool, blockID);
            String message = LanguageRegistry.instance().getStringLocalization("command.veinminer.blocklist.remove");
            sendProperChatToPlayer(senderPlayer, message, blockID.id, blockID.metadata, toolString);
        }
    }

    private void runCommandToollist(EntityPlayerMP senderPlayer, String[] astring) {
        ConfigurationSettings configSettings = MinerServer.instance.getConfigurationSettings();

        ToolType tool = commandTool(astring, "toollist");
        String toolString = astring[1];

        commandAction(astring, "toollist");
        String action = astring[2];

        if(astring.length < 4) {
            showUsageError(senderPlayer, "command.veinminer.toollist.itemerror", toolString, action);
        }

        int toolId;
        try{
            toolId = Integer.parseInt(astring[3]);
        }
        catch(NumberFormatException e) {
            toolId = -1;
        }

        if(toolId <= 0) {
            // String is not in proper format
            showUsageError(senderPlayer, "command.veinminer.toollist.itemerror", toolString, action);
        }

        if("add".equals(action)) {
            configSettings.addTool(tool, toolId);
            String message = LanguageRegistry.instance().getStringLocalization("command.veinminer.toollist.add");
            sendProperChatToPlayer(senderPlayer, message, toolId, toolString);
        }
        else if("remove".equals(action)) {
            configSettings.removeTool(tool, toolId);
            String message = LanguageRegistry.instance().getStringLocalization("command.veinminer.toollist.remove");
            sendProperChatToPlayer(senderPlayer, message, toolId, toolString);
        }
    }

    private void runCommandBlocklimit(EntityPlayerMP senderPlayer, String[] astring) {
        if(astring.length == 1) {
            showUsageError(senderPlayer, "command.veinminer.blocklimit");
        }

        int newBlockPerTick = 0;
        try {
            newBlockPerTick = Integer.parseInt(astring[1]);
        }
        catch (NumberFormatException e) {
            showUsageError(senderPlayer, "command.veinminer.blocklimit");
        }

        MinerServer.instance.getConfigurationSettings().setBlockLimit(newBlockPerTick);

        int actualBlockPerTick = MinerServer.instance.getConfigurationSettings().getBlockLimit();
        String message = LanguageRegistry.instance().getStringLocalization("command.veinminer.blocklimit.set");
        sendProperChatToPlayer(senderPlayer, message, actualBlockPerTick);
    }

    private void runCommandRange(EntityPlayerMP senderPlayer, String[] astring) {
        if(astring.length == 1) {
            showUsageError(senderPlayer, "command.veinminer.range");
        }

        int newRange = 0;
        try {
            newRange = Integer.parseInt(astring[1]);
        }
        catch (NumberFormatException e) {
            showUsageError(senderPlayer, "command.veinminer.range");
        }

        MinerServer.instance.getConfigurationSettings().setRadiusLimit(newRange);

        int actualRange = MinerServer.instance.getConfigurationSettings().getRadiusLimit();
        String message = LanguageRegistry.instance().getStringLocalization("command.veinminer.range.set");
        sendProperChatToPlayer(senderPlayer, message, actualRange);
    }

    private void runCommandPerTick(EntityPlayerMP senderPlayer, String[] astring) {
        if(astring.length == 1) {
            showUsageError(senderPlayer, "command.veinminer.pertick");
        }

        int newRate = 0;
        try {
            newRate = Integer.parseInt(astring[1]);
        }
        catch (NumberFormatException e) {
            showUsageError(senderPlayer, "command.veinminer.pertick");
        }

        MinerServer.instance.getConfigurationSettings().setRadiusLimit(newRate);

        int actualRate = MinerServer.instance.getConfigurationSettings().getRadiusLimit();
        String message = LanguageRegistry.instance().getStringLocalization("command.veinminer.pertick.set");
        sendProperChatToPlayer(senderPlayer, message, actualRate);
    }

    private void runCommandHelp(EntityPlayerMP senderPlayer, String[] astring) {
        if(astring.length > 1) {
            if(astring[1].equals(commands[COMMAND_MODE])) {
                sendProperChatToPlayer(senderPlayer, "command.veinminer.help.enable1");
                sendProperChatToPlayer(senderPlayer, "command.veinminer.help.enable2");
                sendProperChatToPlayer(senderPlayer, "command.veinminer.help.enable3");
                sendProperChatToPlayer(senderPlayer, "command.veinminer.help.enable4");
                sendProperChatToPlayer(senderPlayer, "command.veinminer.help.enable5");
            }
        }
        else {
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help1");
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help2");
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help3");
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help4");
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help5");
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help6");
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help7");
            sendProperChatToPlayer(senderPlayer, "command.veinminer.help8");
        }
    }

    public List addTabCompletionOptions(ICommandSender par1ICommandSender, String[] arguments) {
        switch (arguments.length) {
            case 1:
                return getListOfStringsMatchingLastWord(arguments, commands);
            case 2:
                if(arguments[0].equals(commands[COMMAND_MODE])) {
                    return getListOfStringsMatchingLastWord(arguments, modes);
                }
                else if(arguments[0].equals(commands[COMMAND_BLOCKLIST]) || arguments[0].equals(commands[COMMAND_TOOLLIST])) {
                    String[] tools = { "pickaxe", "axe", "shovel" };

                    return getListOfStringsMatchingLastWord(arguments, tools);
                }
                else if(arguments[0].equals(commands[COMMAND_TOOLLIST])) {
                    return getListOfStringsMatchingLastWord(arguments, commands);
                }
            case 3:
                if(arguments[0].equals(commands[COMMAND_BLOCKLIST]) || arguments[0].equals(commands[COMMAND_TOOLLIST])) {
                    String[] actions = { "add", "remove" };

                    return getListOfStringsMatchingLastWord(arguments, actions);
                }
        }
        return null;
    }

    @Override
    public String getCommandUsage(ICommandSender par1ICommandSender) {
        return LanguageRegistry.instance().getStringLocalization("command.veinminer");
    }
}
