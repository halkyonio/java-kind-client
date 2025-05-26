package dev.snowdrop.command.pkg;

import picocli.CommandLine;

@CommandLine.Command(
    name = "package",
    description = "Commands for managing software packages.",
    mixinStandardHelpOptions = true,
    subcommands = {
        InstallPackage.class,
        DeletePackage.class
    })
public class PackageCommand implements Runnable {

    @CommandLine.Option(names = {"-k", "--kube-config"}, description = "The path of the kubeconfig file", scope = CommandLine.ScopeType.INHERIT)
    String kubeConfigPath;

    @CommandLine.Option(
        names = {"-n", "--namespace"},
        description = "Namespace where to deploy the package",
        defaultValue = "default",
        scope = CommandLine.ScopeType.INHERIT)
    String namespace;

    @Override
    public void run() {
        System.out.println("Package management commands:");
        System.out.println("Use 'mycli package --help' to see available options.");
    }
}
