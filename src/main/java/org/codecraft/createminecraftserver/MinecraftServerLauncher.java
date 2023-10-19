package org.codecraft.createminecraftserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MinecraftServerLauncher {
    private static final Logger logger = LogManager.getLogger(MinecraftServerLauncher.class);
    private static final String DATA_DIRECTORY = convertToAbsolutePath(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "ServerData");
    private static final String ARGS_FILE_PATH = DATA_DIRECTORY + File.separator + "args.txt";

    public static void main(String[] args) {
        checkAndOpenTerminal();
        isDockerInstalled();
        MinecraftServerLauncher minecraftServerLauncher = new MinecraftServerLauncher();
        minecraftServerLauncher.launchServer(args);
    }

    public void launchServer(String[] args) {
        // Check if args.txt file exists and contains the required arguments
        if (!isArgsFileValid() || args.length != 0) {
            // Prompt for Minecraft version
            String minecraftVersion = args.length > 0 ? args[0] : promptForInput("Enter the Minecraft version for your modpack");

            // Prompt for Modloader
            String modloader = args.length > 1 ? args[1] : promptForInput("Enter the Modloader for your modpack (FORGE or FABRIC)");

            // Prompt for EULA acceptance
            String eulaAccepted = args.length > 2 ? args[2] : promptForInput("Have you accepted the Minecraft EULA? (Enter 'Y' or 'N')");

            // Validate user input for EULA acceptance
            while (!eulaAccepted.equalsIgnoreCase("Y") && !eulaAccepted.equalsIgnoreCase("N")) {
                logger.info("Invalid input! Please enter 'Y' or 'N'");
                eulaAccepted = promptForInput("Have you accepted the Minecraft EULA? (Enter 'Y' or 'N')");
            }

            // Convert user input for EULA acceptance to boolean
            boolean eula = eulaAccepted.equalsIgnoreCase("Y");

            if (!eula) {
                logger.info("You must accept the Minecraft EULA to launch the server.");
                logger.info("Press Enter to exit...");
                new Scanner(System.in).nextLine();
                System.exit(0);
            }

            // Determine the type of Modloader (Forge or Fabric)
            if (modloader.equalsIgnoreCase("FORGE")) {
                // Prompt for Forge version
                String forgeVersion = promptForInput("Enter the Forge version for your modpack");

                // Save the arguments to the args.txt file
                saveArgsToFile(minecraftVersion, modloader, eulaAccepted, forgeVersion);

                // Docker run command for Forge mode with user-provided inputs
                String dockerCommand = "docker run --rm -it -v \"" + DATA_DIRECTORY + ":/data\" -e TYPE=FORGE -e MEMORY=4G -e VERSION=" + minecraftVersion + " -e FORGE_VERSION=" + forgeVersion + " -p 25565:25565 -e EULA=" + eula + " --name mc itzg/minecraft-server";

                executeDockerCommand(dockerCommand);
            } else if (modloader.equalsIgnoreCase("FABRIC")) {
                // Save the arguments to the args.txt file
                saveArgsToFile(minecraftVersion, modloader, eulaAccepted);

                // Docker run command for Fabric mode with user-provided inputs
                String dockerCommand = "docker run --rm -it -v \"" + DATA_DIRECTORY + ":/data\" -e TYPE=FABRIC -e MEMORY=4G -e VERSION=" + minecraftVersion + " -p 25565:25565 -e EULA=" + eula + " --name mc itzg/minecraft-server";

                executeDockerCommand(dockerCommand);
            } else {
                logger.info("Invalid Modloader! Only 'FORGE' and 'FABRIC' are supported.");
                logger.info("Press Enter to exit...");
                new Scanner(System.in).nextLine();
                System.exit(0);
            }
        } else {
            // Read the arguments from the args.txt file
            logger.info("Saved arguments found, using those");
            String[] argsFromFile = readArgsFile();

            if (argsFromFile.length < 3) {
                logger.info("Invalid args.txt file.");
                return;
            }

            String minecraftVersion = getValueFromArgs(argsFromFile, "minecraftVersion");
            String modloader = getValueFromArgs(argsFromFile, "modLoader");
            String eulaAccepted = getValueFromArgs(argsFromFile, "eulaAccepted");

            // Convert user input for EULA acceptance to boolean
            boolean eula = eulaAccepted.equalsIgnoreCase("Y");

            if (!eula) {
                logger.info("You must accept the Minecraft EULA to launch the server.");
                logger.info("Press Enter to exit...");
                new Scanner(System.in).nextLine();
                System.exit(0);
            }

            // Determine the type of Modloader (Forge or Fabric)
            if (modloader.equalsIgnoreCase("FORGE")) {
                // Prompt for Forge version
                String forgeVersion = promptForInput("Enter the Forge version for your modpack");

                // Save the arguments to the args.txt file
                saveArgsToFile(minecraftVersion, modloader, eulaAccepted, forgeVersion);

                // Docker run command for Forge mode with user-provided inputs
                String dockerCommand = "docker run --rm -it -v " + convertToAbsolutePath(DATA_DIRECTORY) + ":/data -e TYPE=FORGE -e MEMORY=4G -e VERSION=" + minecraftVersion + " -e FORGE_VERSION=" + forgeVersion + " -p 25565:25565 -e EULA=" + eula + " --name mc itzg/minecraft-server";

                executeDockerCommand(dockerCommand);
            } else if (modloader.equalsIgnoreCase("FABRIC")) {
                // Save the arguments to the args.txt file
                saveArgsToFile(minecraftVersion, modloader, eulaAccepted);

                // Docker run command for Fabric mode with user-provided inputs
                String dockerCommand = "docker run --rm -it -v " + convertToAbsolutePath(DATA_DIRECTORY) + ":/data -e TYPE=FABRIC -e MEMORY=4G -e VERSION=" + minecraftVersion + " -p 25565:25565 -e EULA=" + eula + " --name mc itzg/minecraft-server";

                executeDockerCommand(dockerCommand);
            } else {
                logger.info("Invalid Modloader! Only 'FORGE' and 'FABRIC' are supported.");
                logger.info("Press Enter to exit...");
                new Scanner(System.in).nextLine();
                System.exit(0);
            }
        }
    }

    private void saveArgsToFile(String minecraftVersion, String modloader, String eulaAccepted, String... additionalArgs) {
        try {
            File argsFile = new File(ARGS_FILE_PATH);
            if (!argsFile.exists()) {
                createDataDirectory();
                argsFile.createNewFile();
            }

            String[] argsContent = new String[3 + additionalArgs.length];
            argsContent[0] = "minecraftVersion=" + minecraftVersion;
            argsContent[1] = "modLoader=" + modloader;
            argsContent[2] = "eulaAccepted=" + eulaAccepted;
            for (int i = 0; i < additionalArgs.length; i++) {
                argsContent[i + 3] = additionalArgs[i];
            }

            Files.write(Paths.get(ARGS_FILE_PATH), Arrays.asList(argsContent));
        } catch (IOException e) {
            logger.info("An error occurred while saving the arguments to the args.txt file.");
            logger.info(e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isDockerInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("docker");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static void createDataDirectory() {
        File dataDirectory = new File(DATA_DIRECTORY);
        if (!dataDirectory.exists()) {
            logger.info("Attempting to create data directory at " + DATA_DIRECTORY);
            boolean mkdirs = dataDirectory.mkdirs();
            if (!mkdirs) {
                logger.info("Unable to create data directory.");
                System.exit(1);
            } else {
                logger.info("Done!");
            }
        }

        File argsFile = new File(ARGS_FILE_PATH);
        if (!argsFile.exists()) {
            try {
                argsFile.createNewFile();
            } catch (IOException e) {
                logger.info("An error occurred while creating the args.txt file.");
                logger.info(e.getMessage());
                System.exit(1);
            }
        }
    }

    private static boolean isArgsFileValid() {
        File argsFile = new File(ARGS_FILE_PATH);
        if (!argsFile.exists()) {
            return false;
        }

        try {
            String[] args = Files.readAllLines(Paths.get(ARGS_FILE_PATH)).toArray(new String[0]);
            String argsContent = String.join("\n", args);

            return argsContent.matches("(?s).*minecraftVersion.*") &&
                    argsContent.matches("(?s).*modLoader.*") &&
                    argsContent.matches("(?s).*eulaAccepted.*");
        } catch (IOException e) {
            return false;
        }
    }

    private static String promptForInput(String message) {
        System.out.println(message + ": ");
        return new Scanner(System.in).nextLine();
    }

    private static String[] readArgsFile() {
        try {
            return Files.readAllLines(Paths.get(ARGS_FILE_PATH)).toArray(new String[0]);
        } catch (IOException e) {
            return new String[0];
        }
    }

    private static String getValueFromArgs(String[] args, String key) {
        for (String arg : args) {
            if (arg.matches("(?i).*" + key + ".*")) {
                return arg.replaceAll("(?i)" + key + "=", "");
            }
        }
        return "";
    }

    private static void executeDockerCommand(String dockerCommand) {
        logger.info("Attempting to start docker process with command " + dockerCommand);
        Process process = null;
        try {
            List<String> command = Arrays.asList(dockerCommand.replace("\\", "/").split("\\s+"));
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();
            process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.info("An error occurred while running the Minecraft server:");
            logger.info(e.getMessage());
            System.exit(1);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private static String convertToAbsolutePath(String path) {
        return new File(path).getAbsolutePath();
    }

    private static void checkAndOpenTerminal() {
        if (System.console() == null) {
            String jarFilePath = MinecraftServerLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String jarFileName = new File(jarFilePath).getName();
            // Not running from a terminal, open a new terminal window
            try {
                ProcessBuilder processBuilder = null;
                String os = System.getProperty("os.name").toLowerCase();
                boolean isExecutable = isExecutableExtension(os, getFileExtension(jarFileName));

                if (isExecutable) {
                    if (os.contains("win")) {
                        // For Windows
                        processBuilder = new ProcessBuilder("cmd", "/c", "start", "cmd.exe", "/K", "java -jar " + jarFileName);
                    } else if (os.contains("mac")) {
                        // For macOS
                        processBuilder = new ProcessBuilder("osascript", "-e", "tell app \"Terminal\" to do script \"cd " + System.getProperty("user.dir") + " && java -jar " + jarFileName + "\"");
                    } else if (os.contains("nix") || os.contains("nux") || os.contains("bsd")) {
                        // For Linux/Unix
                        processBuilder = new ProcessBuilder("x-terminal-emulator", "-e", "java", "-jar", jarFileName);
                    } else {
                        System.out.println("Unsupported operating system");
                        return;
                    }
                } else {
                    if (os.contains("win")) {
                        // For Windows
                        processBuilder = new ProcessBuilder("cmd", "/c", "start", "cmd.exe", "/K", "java -jar " + jarFileName);
                    } else if (os.contains("mac")) {
                        // For macOS
                        processBuilder = new ProcessBuilder("osascript", "-e", "tell app \"Terminal\" to do script \"cd " + System.getProperty("user.dir") + " && java -jar " + jarFileName + "\"");
                    } else if (os.contains("nix") || os.contains("nux") || os.contains("bsd")) {
                        // For Linux/Unix
                        processBuilder = new ProcessBuilder("x-terminal-emulator", "-e", "java", "-jar", jarFileName);
                    } else {
                        System.out.println("Unsupported operating system");
                        return;
                    }
                }

                if (processBuilder != null) {
                    processBuilder.start();
                    killFirstInstance();
                    System.exit(0); // Terminate the current instance
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    private static boolean isExecutableExtension(String os, String fileExtension) {
        if (os.contains("win") && fileExtension.equals("exe")) {
            return true;
        } else if (os.contains("mac") && fileExtension.equals("app")) {
            return true;
        } else if ((os.contains("nix") || os.contains("nux") || os.contains("bsd"))) {
            return false;
        }

        return false;
    }

        private static void killFirstInstance() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/IM", "java.exe", "/F");
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
