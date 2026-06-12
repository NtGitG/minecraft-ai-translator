package fr.ntgitg.mineglot.utils.help;

import java.util.List;

public class HelpData {
    public List<HelpCommand> commands;

    public static class HelpCommand {
        public String usage;
        public String description;
    }
}
