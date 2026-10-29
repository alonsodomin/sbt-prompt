{
  description = "sbt-prompt development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/26.05";
    flake-parts.url = "github:hercules-ci/flake-parts";
    treefmt-nix.url = "github:numtide/treefmt-nix";
    devshell.url = "github:numtide/devshell";
  };

  outputs =
    inputs@{
      flake-parts,
      treefmt-nix,
      devshell,
      ...
    }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];

      imports = [
        treefmt-nix.flakeModule
        devshell.flakeModule
      ];

      perSystem =
        { pkgs, ... }:
        {
          treefmt = import ./treefmt.nix;

          devshells.default = devshell.lib.importTOML ./devshell.toml;
        };
    };
}
