package dev.snowdrop.command.component;

import picocli.CommandLine;

@CommandLine.Command(
    name = "package",
    description = "Commands for managing software packages.",
    mixinStandardHelpOptions = true,
    subcommands = {
        InstallPackage.class
    })
public class PackageCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Package management commands:");
        System.out.println("Use 'mycli package --help' to see available options.");
    }
}
