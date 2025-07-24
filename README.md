# a space project
Welcome to A Space Project. A project involving space...
> Get a realistic sense of the cosmic scale; there's literally dozens of planets and traveling between them could take up to entire minutes! Enjoy Galactic-Space-Exploration-in-a-sate-of-the-art-hyper-realistic-physics-simulation-of-the-entire-universe!

![screenshot](/Capture.PNG?raw=true)
![screenshot](/asteroid_shatter.gif?raw=true)
***Game is in pre-alpha prototype phase. More of a sandbox than a game, there is not much content yet***

## Features
* Fly around the star system in a spaceship
* Discover a plethora of astronomical bodies including:
    * Unary star systems, Binary star systems, Trinary star systems, and even ~~Quadri... quatro? quadrino-ary?~~
    * lonely rogue planets who lost their sun :(
    * Destructible asteroids using cutting edge - ***t r i ▲ n g l e s*** -
* Mine asteroids for resources.
* Dock at Space Station to sell resources. Upgrade ship.
* Faster Than Light Travel! (yeah it's real, cuz like quantum anti-dark matter n' stuff yo)
* Lasers?
   * Lasers. 
* ~~Fight against other ship~~ (disabled for now)
  * combat broken placeholder AI while I figure out the rest of the engine.
* Land on and explore finite (toroidally wrapped) planets (in theory, probably out of scope).
* Controller Support (works in game but not all menus)
   * hot plugging!
* Sound?
  * there is no sound in the vacuum of space silly
* ~~Unit Tests~~
   * pfft... my code is perfect. the first time. every time.
* Developer Tools (in progress)
* Cross-Platform Desktop
  * Linux, Windows, OSX
  * Official Steam Deck support!
* Feature Creep and Unrealistic Scope!
   * Multiplayer is out of current scope :(


### Controls
| Control                        | Desktop       | Controller/Gamepad |
|------------------------------- | ------------  | ------------------ |
| Aim                            | Mouse         | Left Stick         |
| Movement                       | WASD          | Left Stick + L1/R1 |
| Boost                          | Space         | A                  |
| Brakes                         | S             | X                  |
| Attack: Shoot                  | Left-Click    | RT                 |
| Defense: Shield                | Shift         | LT                 |
| Defense: Dodge (Barrel Roll)   | Double Tap A/D | Double Tap R1/L1  |
| Interact                       | E             | D-Pad Down         |
| Cycle Equiped Tool             | Q             | D-Pad Right        |
| Engage HyperDrive              | Hold 1        | Hold B             |
| Enter/Exit vehicle             | G             | Y                  |
| Zoom                           | Scroll Wheel  | Right JoyStick     |
| Reset Zoom                     | Middle-Click  | Click in Right stick |
| Toggle Map State               | M             |                      |
| Toggle HUD                     | H             |                      |
| Full screen                    | F11           |                      |
| Menu (Pause)                   | Escape        | Start                |
| MSAA (Multisample Anti-Alias)  | F7            |                      |
| Vsync                          | F8            |                      |
| ECS Debug Viewer               | F9            |                      |
| Misc debug keys | too lazy to document, won't be permanent |


## Current Status
A work in progress engine toy sandbox thing: scaffolding for what will hopefully one day be a game.
The code is bit rough in some places, littered with todo's, half-baked features, and the occasional bug.
For more documentation and current status see:

wiki: https://github.com/0XDE57/SpaceProject/wiki

project: https://github.com/users/0XDE57/projects/1


## License
Apache 2.0: see [LICENSE.md](/LICENSE.md)

Credit appreciated. Contributions welcome!

## Thank!

Thanks to the incredibly knowledgable and helpful libGDX community for guiding me along the way. This game would not exist with out you.

Thanks to L1T for hosting Devember.

Thanks to all the developers who built the open source technology that powers this game:


## Libraries
- [libGDX](https://github.com/libgdx/libgdx)
- [Box2D](https://box2d.org/)
- [Ashley](https://github.com/libgdx/ashley/wiki)
- [OpenSimplexNoise](https://gist.github.com/KdotJPG/b1270127455a94ac5d19)
- [VisUI](https://github.com/kotcrab/vis-ui)
- [TuningFork](https://github.com/Hangman/TuningFork)


## Building
**General**
* Set up your Development Environment: https://libgdx.com/wiki/start/setup
* Import project in IDE of choice using gradle.
* If a "File not found" error occurs, check the working directory. Append "\assets" to the working directory in run configurations.


**IntelliJ**
* Desktop
  * create Run Configuration
  * main class = com.spaceproject.desktop.DesktopLauncher
  * use classpath of module 'desktop'
  * working directory = ...\SpaceProject\assets
      * (must ensure working directory includes assets so data like fonts, particles, shaders, configs can be loaded)
  * build and run!

