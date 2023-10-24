# ForgeRunner

> [!WARNING] 
> No! This cannot load Quark, Create or any other mod on Fabric!
> This is a simple experiment!!!

A (questionable) attempt at loading Forge mods on Fabric!

Currently can:

- [x] Basic mod discovery.
- [x] Super basic 1-mod metadata conversion.
- [x] Basic mod injection.

What needs to be done:

- Conflict resolution.
- JarJar.
- Remapping to `intermediary`
- - AsmApi and whatnot.
- Adapting classes to a non-patched version of the game.
- - Field getters.
- - Custom constructors.
- - Forge methods.
- - etc.
- Entrypoint discovery
- - Main, Client, Server.
- - PreLaunch.
- Mixin fixes & CoreMods.
- - LVTs, patched methods & constructors & more.
- (Some parts of) Forge & friends API.
- - RuntimeEnums, UnsafeHacks, Services, etc.
- and more, and more, and more.

If you want a working version of this, checkout [Sinytra Connector](https://github.com/Sinytra/Connector). As, unlike this, you can actually play with it right now!

***

Includes [Night Config](https://github.com/TheElectronWill/night-config) licensed under [GNU Lesser General Public License v3.0](https://github.com/TheElectronWill/night-config/blob/master/LICENSE)
