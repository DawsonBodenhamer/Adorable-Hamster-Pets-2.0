# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.6.1] - 2025-04-27

# **The Punchy Patch**

In addition to a few bug fixes and a bunch of new quality-of-life features (including hamster armor trims!), this patch introduces a new batch of super cute, first-person animations to your rodent-handling experience, courtesy of the amazing [**Punchy**](https://modrinth.com/mod/punchy-fpa) mod! I've been working closely with [**@Dev Punchy Man**](https://modrinth.com/mod/punchy-fpa) to overhaul how it feels to pick up, pet, yeet, and dismount your hamsters.

### Added
- **New First-Person Animations (Requires Punchy v2.6.0+)**
  - **Petting/Tickling**
    - You can finally pet your hamster! If you stare affectionately at your hamster for a **while sneaking** (about 15 seconds on average), you might just reach down, pick it up and give it some tickles. Comes with a new animation where the hamster flips over on your hand and asks for a belly rub! Also added a dedicated "Pet Hamster" keybind (unbound by default) for when you don't feel like waiting for the random chance to kick in.
    - For those with the patience of a fruit bat, pressing your "Pet Hamster" key or clicking your mouse will cancel the petting animation. You don't need to be sneaking to use the keybind.
    - If the hamster was sitting before you picked it up to pet it, it will remain seated when you place it back down, making this a great way to manually re-position tamed hamsters around your base.
  - **Shoulder Mounting (Dynamic)**
    - Luring a hamster to your shoulder is no longer a boring teleport. You will now physically lift them up, complete with the hamster adorably bouncing on your hands or running up your arm. Three new unique animations; dynamically changing depending on their destination (left shoulder, right shoulder, head).
  - **The Yeet Queuing System (Dynamic)**
    - Throwing a hamster is no longer instant. Holding down the throw key (`G`) grabs a specific hamster off your shoulder and holds it in front of your face.
      - While queued up, the hamster also plays its own new animation (only available with Punchy v2.6.0+) where it eagerly wiggles its butt and kicks its back feet in anticipation of being hurled through the air.
      - Releasing the throw key (`G`) executes the throw, which is immediately followed by a new Punchy animation where you wave goodbye to your furry projectile.
      - If you release the key before the short charging period (`15 ticks`) is over, you safely abort the throw. Your character will politely place the hamster back onto the exact shoulder it came from. This charging period of `15 ticks` is not configurable, due to its duration needing to match the animation. But it's quite short— only `0.75 seconds`— so hopefully it won't impact gameplay too much.
- **Hamster Yeet Mechanics**
  - Added a `Downward Force (Gravity)` slider to the config, allowing you to fine-tune how far hamsters fly without needing to increase their initial thrust. (Useful for the Flashback mod, which gets buggy when entities are moving quickly).
  - **FOV Zoom**
    - Added a smooth FOV zoom effect while queueing the Hamster Yeet, identical to drawing a vanilla bow. This FOV zoom will help indicate the new charging period for anyone who doesn't have Punchy installed, since the new animations cannot work without it.
  - **Cooldown Recovery**
    - If you're impatient, feeding your hamster will now incrementally reduce its throw cooldown (similar to how feeding accelerates baby growth).
  - A new action bar message will let you know when the hamster has recovered from its concussion and is ready for launch.
- **Configurable Aggression States**
  - Hamsters now have three distinct aggression states controlled by their diet.
  - **Pacifist:** Feed them any vanilla flower (e.g., `#minecraft:flowers`). They become total hippies and will refuse to attack anything, even if you are being actively mauled.
  - **Standard:** Feed them Sunflower Seeds to factory-reset them back to their normal, wolf-like defensive behavior.
  - **Menace:** Feed them a Spider Eye to unleash their inner demon. They will actively hunt down anything on the configurable "Menace Targets" list (which defaults to all monsters and bosses).
  - All trigger items are fully configurable, and changing a hamster's state produces a visual and audio confirmation.
  - **Hamster Tips Guidebook Entry**
    - The *Hamster Tips* guidebook now features a dedicated "Aggression States" page to explain the new mechanics. It utilizes dynamic text injection to accurately display your configured trigger items, ensuring the book is always up-to-date even if a modpack creator changes the required diets.
  - **Pacifist Break:** Added a `Pacifist Break on Attack` config toggle (false by default). When enabled, a passive hamster will automatically revert to neutral if it sees you attacking something.
- **Armor Trims**
  - Hamster Armor can now be customized with vanilla armor trims in the Smithing Table.
  - The following trims are currently available: Coast, Border, Vex, Eye, Sentry, Wild
  - They will naturally glow in the dark and automatically have emission with shader mods like Iris.
  - Added an `Emissive Armor Trims` toggle in the client config so you can disable the glowing effect if you hate fun and prefer a non-luminescent reality.
- **"Hamtaro" Easter Egg**
  - Renaming a hamster "Hamtaro" gives it a special texture. This functions identically to the "Sweet Potato" easter egg. Both base textures provided by [**@jimcerberus**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com)!
- **New "Panda" Overlay Pattern**
  - Added a new "Panda" fur overlay pattern to the genetics engine, allowing you to breed panda-like hamsters. To get a "Panda" hamster, you'll need a Black base coat and a white overlay with the new "Panda" pattern. These can't be found in the wild, since black hamsters do not spawn with white overlays in the wild (breaks their camouflage). Thanks to [**@jimcerberus**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com) for the Panda inspiration!
- **Cheese Music Disc**
  - A legendary new music disc with The Adorable Hamster Pets theme song, created by yours truly. Can only be obtained by orchestrating a high-velocity, terminal collision between an airborne hamster and a Charged Creeper.
  - If playing in a Jukebox, nearby hamsters will dance to the music.
  - Added the "Moist Tones" advancement.
- **Free Bed Respawn Toggle**
  - Added a config toggle to allow hamsters to respawn at their linked beds indefinitely without requiring a tribute item charge (like a Totem of Undying). Disabled by default.
- **Dynamic Trees Compatibility**
  - Added out-of-the-box support for [**Dynamic Trees**](https://modrinth.com/mod/dynamictrees) via some new config settings.
  - Replaced hardcoded oak leaves checks with a new `Heistable Leaves` list in the config, allowing modpack makers to easily add other modded leaves or trees to the heist feature.
  - Added a new `Heistable Logs` list to the `Tree Heist Settings` config, similar to the `Heistable Leaves` list. This means you can now start a tree heist by throwing the hamster at a branch or the trunk.
  - Developed a dumber but more compatible canopy-mapping algorithm that activates automatically if `Dynamic Trees` is installed, ensuring heist still works without the vanilla leaves' internal `distance from trunk` property.
  - **Acorn Note:** The Dynamic Trees mod adds their own type of acorns and drop methods. When that mod is installed, the only way to get the specific acorns from my mod is through the Tree Heist.
- **Dehydration and NutritionZ Compatibility**
  - Added built-in datapacks for the [**Dehydration**](https://modrinth.com/mod/dehydration) and [**NutritionZ**](https://modrinth.com/mod/nutritionz) mods. Cucumbers will now hydrate you, and Cheese will finally clog your arteries properly.
- **Combustible Acorns**
  - Acorns can now be used as furnace fuel, smelting exactly the same amount of items as a vanilla stick.
  - Alternatively, placing an Acorn in the top slot of a furnace will smelt it down into a piece of Charcoal.
- **World Gen Compatibility Enhancements**
  - Added massive out-of-the-box biome compatibility configuration data for [**Oh The Biomes We've Gone**](https://modrinth.com/mod/oh-the-biomes-weve-gone), ensuring hamster variants properly disperse across their beautiful landscapes. Huge thanks to [**@jlk2003r**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com)  for helping configure these lists!
- **Create Mod Compatibility**
  - Added out-of-the-box recipe compatibility for [**Create: Bitterballen**](https://modrinth.com/mod/create-bitterballen) and [**Create Crafts & Additions**](https://modrinth.com/mod/createaddition).
  - You can now mill, crush, compact, roast, and smoke AHP Sunflower Seeds to progress through the Bitterballen tech tree seamlessly, bypassing the need to hunt down vanilla sunflowers. Thanks to [**@jlk2003r**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com) for teaching me about Create so I knew what recipes to add!
- **Armor Tier Jumping** 
  - You can now upgrade Hamster Armor between any tiers using the Smithing Table (e.g., Iron to Diamond, Diamond to Netherite). Previously, all upgrades had to start from base Acorn Armor. You can also downgrade it... should you decide to do so.
- **Infinite Bed Respawns**
  - Added a new `Tribute One-Time Use` config setting. When enabled, a bed only requires the tribute item (Totem of Undying by default) once, permanently unlocking infinite respawns for that specific bed and hamster.
- **Taming Feedback**
  - The mod will now provide snarky chat feedback if you try to tame a hamster incorrectly (such as not sneaking or offering them the wrong food) and will point you to the guidebook for help.
- **Global Notification Toggle**
  - Added a `Server Disable Announcements` config setting. Server owners can now globally disable the notification bell icon for all connected players.
- **Smoker Support**
  - Green Beans can now be cooked into Steamed Green Beans using a Smoker, which cooks them twice as fast as a standard furnace.
- **Compostable Sunflowers**
  - The custom Adorable Hamster Pets Sunflower can now be placed in a composter (matching the 65% compost chance of vanilla sunflowers).
- **Plantable Acorns**
  - Acorns can now be planted directly on dirt/grass to grow vanilla Oak Trees. Once placed they will become Oak Saplings.

### Changed
- **Dismount Keybinding Simplification**
  - Removed the confusing config toggle that forced players to enable a custom keybind in the config before they could rebind the "Dismount Hamster" key in the `Controls > Key Binds` menu. The keybind is now permanently exposed.
  - The "Dismount Hamster" keybind now utilizes an unbound fallback system. By default, the keybind is set to `Unbound`, and the game will naturally listen to your vanilla `Sneak` key to dismount hamsters. However, if you manually assign a key to it in the controls menu, it will override the sneak behavior and listen exclusively to your custom key.
  - The default "Button-Press Behavior" config setting has been changed from `Single Press` to `Double Tap` to prevent accidental dismounts when sneaking near ledges. Originally it was set to single press to mimic vanilla parrot behavior, but it's just so annoying. I finally can't stand it anymore. Lol.
  - Added a `Custom Key Behavior Override` config toggle (true by default). When you bind a custom key for dismounting, it automatically overrides the `Double Tap` behavior and makes it a `Single Press`. I assume if you assigned a dedicated key to it, you don't want to have to tap it twice.
- **Keybind Names**
  - Renamed several of the key binds to make the `Controls > Key Binds` menu more intuitive.
- **The Hamster Yeet**
  -  Throwing a hamster is no longer canceled/prevented if the player's crosshair is over non-solid blocks like tall grass, flowers, sugar cane etc.
  - Reduced the default gravity applied to thrown hamsters by 30%, resulting in naturally longer, flatter arcs out-of-the-box. Hamsters will feel less "heavy" now during throws.
- **Hamster Tips Guidebook**
  - "The Great Escape" has been renamed to "Beds & Wander Mode," and "Acorn Armor" has been renamed to "Tree Heist & Armor" so you guys won't keep coming into my Discord server asking how the beds work and where to find acorns 😂
  - This has another super useful perk: Now whenever you search through the guidebook for a specific topic, you can search for things like "bed," "tree heist," or "armor" and those specific entries will pop up in the results.
  - Now utilizes dynamic text injection to accurately display your configured resurrection tribute items. So now if a server owner or modpack creator changes the default item from a Totem of Undying, the guidebook will update itself automatically.
- **Tag Mini-Game**
  - Wild hamsters can no longer initiate a game of tag to prevent interference with taming. Tamed hamsters can still play tag with strangers if enabled in the config.
  - The game will no longer randomly start while you are sneaking, preventing interference with petting.
  - You can now manually start a game of tag on-demand by rapidly sneaking and un-sneaking while maintaining eye contact with a tamed hamster.
  - Since there is now a manual trigger, the chance of the game starting is now much lower. You will need to stare at a hamster for ~15 seconds on average for the game to automatically start.
  - Starting a game of tag now plays a sound, spawns particles, and displays an action bar message to clearly indicate that it is a fun event and not a bug. Don't ask.
- **Genetics & Color Groups**
  - Renamed the `ROSE` color group to `CHERRY` since the only hamster in that group is the "Pearl Rose" hamster, which is basically pink anyway.
  - Added a new `RUST` color group. The "Rust" hamster has officially been evicted from the `ORANGE` category, allowing it to have its own dedicated spawning rules.
- **Spawning Config Overhaul**
  - Reorganized the World Gen config UI to clearly separate the global "Allow/Prevent Spawns" lists from the "Region-Based Color Filters". It now clearly explains the "filter funnel" concept so you know exactly why the Plains environment acts as a catch-all.
  - Expanded the procedural spawning logic from 10 environments to 12, giving modpack makers perfect 1:1 granular control over all 12 hamster color groups.
  - **The Wildcard:** Added a "Priority 1: Wildcard" zone. Allows server owners to surgically extract specific biomes from broader categories and assign them unique colors without overhauling the other lists. (i.e., If you wanted to separate the environments in which Light Gray and Dark Gray hamsters spawn, now you can do so).
  - **Sky Environments:** Added a dedicated zone for floating island biomes to host the `SKY` color group. Vanilla players— no need to freak out; there's still a 15% chance to find `SKY` hamsters instead of `WHITE` in snowy areas.
  - **Cherry Environments:** Extracted Cherry Groves out of the Magical environments so they can be balanced independently. Defaults to 100% `CHERRY` hamsters.
  - **Auburn Environments:** Added a dedicated zone targeting Badlands (and modded autumnal forests with red trees if you like). This is the exclusive new home for `RUST` hamsters. (`ORANGE` hamsters will no longer spawn in Badlands by default).
  - **Re-balanced Weights:** Adjusted the default spawning weights across the `Icy`, `Magical`, and `Snowy` environments to accommodate the new color zones.
- **⚠ Important Config Note for Existing Worlds**
  - If you are updating an existing modpack, your old config files will obstinately hold on to their old settings. This is good, otherwise you would lose all your work every time I pushed out an update. However, this means that in order to see these new default spawn weights, you will need to right-click specific settings in the config screen and select "Restore Defaults." Alternatively, click the "Changes" button (bottom-right corner) and click "Restore Defaults" to reset everything at once (only affects the config from my mod).
- **Hamster Bedding Leaf Particles**
  - Doubled the visual size of the Hamster Bedding particles to better match the leaves on the trees and on the hamster bed itself.
- **Pink Petal Accessories**
  - Added a new config toggle (true by default) to dynamically render Pink Petal accessories on the outside of equipped armor, so your hamster can be safe and fabulous simultaneously.
- **Configurable 3D Visualizer Particles**
  - Added a `Continuous Genetics Cylinder` toggle to the config. When disabled, the 3D bounding cylinder generated by `/ahp spawn_all_bases_3D` will only render for the first second after spawning to reduce clutter.

### Fixed
- **Hamster Yeet & Evilcraft Bug**
  - Resolved an bug that prevented hamsters from being thrown on Forge and NeoForge dedicated servers, caused by environment annotations running on the server thread. Super niche little insect that slipped by because it doesn't exist on Fabric. You should be able to throw your hamsters in multiplayer again!
  - This also resolves a server startup crash when playing with EvilCraft, which attempts to instantiate the projectile to check its blood levels. Lol.
- **Food Item Stacking**
  - Resolved an issue on 1.21.1 where the configurable food items became unstackable in the inventory GUI after being split or spread.
  - Implemented a stable ComponentMap caching system to ensure GUI right-click dragging correctly identifies dynamic food stacks as equal.
- **Sliced Cucumber Feeding**
  - Resolved a bug where already-tamed hamsters would refuse to eat their taming food (e.g. Sliced Cucumber) to heal or breed.
- **Projectile Accessories**
  - Fixed a visual bug where a hamster's equipped armor and accessories would temporarily vanish while they were airborne during a throw.
- **Server Performance**
  - Resolved a few pathfinding and AI issues where tamed hamsters following the player would sometimes experience server-tick lag spikes. Usually you wouldn't notice these unless you had a lot of hamsters following you at once.
- **Wander Mode Override**
  - Resolved a bug where disabling Wander Mode in the global config wouldn't stop already-wandering hamsters from lingering around their beds.
- **Cheese Item Texture**
  - Finally fixed the 99% transparent pixel in the bottom left corner of the cheese item texture. I only recently realized what was causing it to look so strange. Lol
- **Console Log Spam**
  - Resolved an issue where players with internet disabled (or those playing in regions where GitHub is blocked) would receive a massive, screen-filling error stack trace in their console every 5 minutes when the mod silently checked for updates in the background. Background network failures are now politely logged as a single warning line.
  - Resolved an issue where the genetics engine would spam the server console with warnings when attempting to generate wild overlays for hamsters with extremely bright base coats (like Coconut).
- **Sweet Potato Easter Egg**
  - Renaming a hamster to "Sweet Potato" now correctly applies the easter egg texture without accidentally deleting the hamster's eyes, skin layer, and accessories. So that's good.
  - Resolved an issue where the effects would trigger immediately upon renaming a hamster via the GUI, before the screen was closed.
  - Resolved an issue where the affects would not trigger at all on 1.20.1 if a hamster was renamed via the GUI.
- **Missing Vanilla Tags**
  - The custom AHP Sunflower block is now properly registered under the vanilla `#minecraft:flowers` and `#minecraft:tall_flowers` tags. This resolves compatibility issues with mods like Alex's Mobs that rely on these tags to identify flowers in the world.
- **Flashback Mod**
  - Resolved a server crash that occurred when scrubbing through replay timelines using the Flashback mod, caused by missing NBT data on Hamster Beds.
  - Resolved a persistent desync issue where shoulder hamsters would disappear when scrubbing backwards or jumping to different points on the replay timeline. Future replays will hopefully be fixed via a periodic server-side data sync that runs once per second.
- **File Parsing Crashes**
  - Fixed a crash that occurred when reading corrupted `.json` cache files for the supporter perk system. It will now safely ignore the corrupted file and download a fresh copy from the internet.
- **Guidebook Server Synchronization**
  - Resolved an issue where the "Missing Guidebook" chat warning would trigger repeatedly across multiple game sessions when playing on a dedicated server due to a configuration synchronization flaw.
  - Fixed an issue where players were falsely led to believe they could locally disable Guidebook Auto-Delivery on dedicated servers. These settings now properly sync from the server to the client's config UI, reflecting the server's true authority over the book's delivery.
- **Food Interactions**
  - Fixed a bug where players could not toggle their hamster's sitting state if they were holding Hamster Food Mix or other food items.

---

## [3.6.0] - 2025-03-03

# **The Procedural Genetics Update**

Hamsters now utilize a fully procedural and configurable genetics engine with **3,158** new wild variants, **2,285,046** potential breeding outcomes, recessive red eyes, 13 new advancements, and a guidebook update that explains everything. Added comprehensive breeding settings to balance obsessions with server tick speed (looking at you, Janet). Also many bugs were squashed, and hamsters learned to play tag and spit out gifts from their cheeks. Make sure you update Patchouli to the latest version or your game won't launch!

## **→** `/ahp print genetics report` **↓**
```log
  |                                                                                      
  |                      Adorable Hamster Pets Procedural Genetics Engine                
  |      --------------------------------------------------------------------------------
  |      Base Fur Palettes ................. | 45
  |      Base Fur Patterns ................. | x 1
  |      Potential Wild Overlay Types ...... | x 235 (26 Palettes x 9 Patterns + 1 blank)
  |      Potential Breeding Overlay Types .. | x 406 (45 Palettes x 9 Patterns + 1 blank)
  |      Potential Eye Color Types ......... | x 2
  |      Visually Distinct Wild Variants ... | = 3,231
  |        ↑ Filtered: Overlays must...
  |          - Be allowed (WHITE, LIGHT_GRAY, DARK_GRAY, CREAM) ← default: neutrals
  |          - Be brighter than base color
  |          - Be less saturated than base color
  |          - Not clash with the BLUE, LAVENDER color zones
  |      Total Possible After Breeding ..... | = 2,285,046
  |      Number of 3D Color Relationships .. | = 2,610,718,753,581
  |                                                                                                                 
```

### Added
- **Procedural Genetics & Texture System**
  - **Procedural Genetics Engine**
    - Hamsters are no longer pre-defined, hardcoded variants. Every hamster now possesses a fully serialized `HamsterGenome` detailing its precise genetic makeup across six distinct traits.
    - This was accomplished by developing a 3D Hue/Saturation/Brightness (HSB) Cartesian color space. The mod uses this to mathematically map the exact color coordinates of every hamster variant into a three-dimensional neural network, which is dynamically used to calculate genetic relationships, mutations, overlay exclusion rules, and environment-spawning rules on the fly.
    - I got the idea for creating the textures programmatically like this because that's how I made the original textures in Photoshop— using various Gradient Maps applied to a single, grayscale texture. Then I realized Java code can do the same thing!
    - The total number of unique hamster types that spawn in the wild is now **3,231** by default, and the number of genetically inheritable hamster permutations from breeding is now **over 2.2 Million**.
    - **Want to understand the math?** Keep reading below— look for the _"Advanced Spawning & Testing Commands"_ section. I have added all sorts of fancy commands and tools to walk you through the breeding system in a way that is (hopefully) easy to understand.
  - **Dynamic Palette Swapping**
    - Built an optimized, client-side dynamic texture generator. Instead of bloating your hard drive with over 2 million distinct PNG files, the mod dynamically recolors grayscale fur templates at runtime using custom genetic palette hex code data I designed based off my Photoshop workflow, then mixes them with hard-coded PNGs from the community.
    - I realized the original hamster variants were just gradient maps applied to a grayscale texture in Photoshop. So, naturally, I invested an unreasonable amount of effort rebuilding Photoshop's Gradient Map tool inside Java.
    - Does this save file space? Sort of, though tiny PNGs don't take up much space anyway. But it makes the system infinitely scalable. If I add just one new grayscale pattern or community-made texture in the future, the Java math will automatically multiply it across every single other variant in the game. For now, consider it an over-engineered proof of concept.
  - **Community Hamster Textures**
    - Developed a parallel pipeline that automatically scans, analyzes, and genetically categorizes static, community-made PNG textures directly from the mod's JAR file. These static textures are seamlessly integrated into the procedural breeding pool alongside my programmatic colors.
    - The rendering engine can automatically composite custom community textures into overlay masks. This means hamsters can spawn with community-made palettes serving as the color for their overlay spots and splotches!
    - Thanks to [**@jimcerberus**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com), we have 18 brand-new base hamster variants spawning in the wild! (Including *Cheesecake Mocha, Blue Fawn, Pearl Rose, Sable,* and 14 more). You can see them all in-game using the new spawn commands (see below).
  - **Expanded Overlays**
    - Overlays are no longer just white.
    - **Wild Overlays:** Naturally spawning hamsters can now have overlays in any color that is closely related to White, Cream, or Gray, with a few configurable exceptions. To keep things looking natural, the default config ensures wild overlays are always brighter & less saturated than the base coat, Cream is disallowed on a Lavender or Blue base coat, and cave-spawned hamsters can only receive Gray overlays to help them blend in.
    - **Breeding Overlays:** A completely new secondary overlay layer, unlocked exclusively through breeding (45% chance when two first-generation wild hamsters breed). These are chosen based on the midpoint between the parents' base color in the 3D color-space coordinate system, with a bit of jitter so the baby won't always look the same when you breed two parents repeatedly, so the possibilities are nearly endless. These overlays will mathematically avoid copying the same shape/pattern as the wild overlay, to ensure they are not hidden/covered up by it.
- **Breeding & Lifecycle Mechanics**
  - **Breeding Inheritance and Visualization Tool**
    - **Here's how it works:** When breeding two hamsters together, the color for the baby (and its overlay colors) are chosen based on a connected line between the two parents in the 3D color-space coordinate system. This means babies will tend towards the mathematical center, but occasionally one parent's color traits will strongly dominate the outcome, mimicking real-world genetics and providing visual variety within a single hamster family even if you breed the same two parents together repeatedly.
    - **Outcome Visualizer:** You can right-click two hamsters inside the 3D variants layout (`/ahp spawn_all_bases_3D`) with the Hamster Tips guidebook to visualize the genetic probability distribution for their offspring via a cloud of Wax On particles. The particle spawning logic uses the exact same probability math that is used when creating babies, so it gives you a perfectly accurate representation of potential outcomes.
    - **Config Settings and Real-time Controls**
      - **Genetic Variance**: Adjusts how much a baby's base color can deviate from the exact center between its parents. Think of this as the "length" of the line between the parents.
      - **Genetic Mutation Rate**: Adjusts the random color scatter/mutations applied to babies. Think of this as the "thickness" of the line.
      - **Simulated Offspring Per Second**: Adjust the density of the 3D visualizer particle cloud. Each particle represents a potential baby.
      - **Real-time Controls:** While holding the guidebook, you can dynamically tweak the shape of the genetic probability cloud using the arrow keys: Left/Right Arrows adjust Genetic Variance; Up/Down Arrows adjust Genetic Mutation Rate.
      - **Server and Client Sync:** Since the particle visualizer is driven by the literal server-side breeding math, which is driven by the config for your server, and the changes you make with the arrow keys directly alter that config, the changes will take effect immediately for any future babies.
  - **Recessive Eye Genetics**
    - Hamsters now possess dominant (Black) and recessive (Red) eye genetics.
    - Red eyes do not exist in the wild. They are genetically tied to the "diluteness" (brightness/saturation) of the hamster's coat. A fully dilute hamster has up to a 50% chance of spawning with a recessive red eye gene. By selectively breeding highly dilute hamsters, players can uncover carriers (`Br`) and eventually breed Red-Eyed (`rr`) variants. Uses punnet squares.
  - **Baby Growth Mechanics**
    - You can now feed baby hamsters standard hamster food items to accelerate their growth.
    - To balance this with their natural pickiness, a new config option (`Disable Baby Food Refusal`) allows you to bypass their desire for dietary variety specifically for babies if you want. This is turned off by default, so to bypass their pickiness you'll need lots of **Hamster Food Mix** if you want to quickly grow them up.
    - Feeding baby hamsters visually and mechanically accelerates their growth in smooth, continuous increments, similar to vanilla horses. Their "extra big head" proportions scale dynamically as they grow.
  - **Breeding Limitations**
    - Added comprehensive config settings to manage hamster breeding for server owners who want to keep the population under control.
    - Added a global `Enable Breeding` toggle (on by default) to instantly shut down all romance.
    - Added a configurable `Max Breeding Cycles` limit per hamster to stop them from infinitely multiplying (off by default).
    - Added a `Limit Breeding By Player` toggle. (also off by default). Limits can be assigned per-player based on Minecraft days or Real Life days, or simply capped as a lifetime maximum limit. Includes feedback to explain what's happening when attempting to over-feed beyond the limit.
    - Added a `/ahp reset_player_breeding_history` command for server operators to reset their own breeding history.
    - Added a `/ahp reset_hamster_breeding_history` command for server operators to reset a specific hamster's `timesBred` quota to zero.
    - Added a convenient "Reset Breeding History" button in the Config screen that executes the command for the user (requires OP permissions).
  - **Configurable Litter Size**
    - Added to config sliders allowing you to set the minimum and maximum litter size when hamsters get into hanky-panky. The final size of the litter will be a random number between the min and max.
  - **Breeder Whitelist**
    - Added a config list allowing specific players to bypass the global breeding ban. In case your server needs a designated rodent baron.
  - **Post-Breeding Animation**
    - Hamsters will now crouch down and lovingly inspect their baby(s) after the litter is born, spawn heart particles, and make affectionate sounds shortly after successfully contributing to your growing population problem.
  - **Wild Baby Configuration**
    - Added `Babies Spawn Wild` setting. When enabled, babies are born feral and do not inherit their parent's ownership status, letting multiplayer groups decide who claims them.
    - When untamed, babies no longer flee from players, making them easier to manage.
  - **Parent-Following**
    - Babies will now randomly select a parent to follow until adulthood, instead of following their owner.
    - Wild baby hamsters will switch to following the player if their parent is currently mounted on the player's shoulder. They will automatically resume following the parent once it is dismounted.
    - If you want the baby to follow you instead, you must first tame it, then you can break its connection to the parent by right-clicking it with a Lure Item (Cheese by default).
    - Added a config toggle to disable the action bar message that warns you when a newly tamed baby hamster refuses to follow you because it's still attached to its parent.
  - **Age Tracking**
    - Hamsters now track their absolute lifetime age in ticks.
    - Wild hamsters spawn with a random age between 1 and 30 days.
    - Added `/ahp set_age` command to manually override a hamster's age (since older hamsters will start at 0 days old upon updating to this version of the mod). It accepts units (`days`, `months`, `years`) and provides autocomplete suggestions.
    - If you do not specify a target, it will automatically apply to the hamster you are currently looking at.
    - The Jade HUD overlay displays the hamster's age alongside its genetic data.
    - Added a new `Display IRL Age` config setting that dictates how fast the hamster ages. If true, their age progresses at 1/72nd the normal speed (matching the real-world 24-hour cycle).
- **Performance Improvements**
  - **Significant Overall Improvements**
    - Flattened the entire visual render layer stack (base coat, wild overlays, breeding overlays, skin, eyes, armor, and accessories) into a single, dynamically composited texture at runtime.
    - This completely eliminates all secondary `GeoRenderLayer` passes. Every hamster, regardless of its complex genetics or equipped items, now costs exactly one draw call to render (previously it was anywhere from 2 to 8!), resulting in a massive performance boost. Especially noticeable when numerous hamsters are on screen at the same time.
  - **Bare-Bones Ultra Performance Mode**
    - Added a new `Performance Mode` toggle in the config (and an assignable keybind) designed specifically for viewing absurd numbers of hamsters simultaneously without melting your GPU.
    - When enabled, it bypasses the dynamic texture engine entirely (falling back to a single flat texture) and hides almost all the model's geometry.
    - The hamsters essentially become grayscale cubes, stripping away nearly all matrix-calculation overhead.
    - The Jade HUD overlay will still display their exact genetic information, because the server never forgets what they are actually supposed to look like.
- **New Config Settings**
  - **Wild Overlay Configurability**
    - Added new `Allowed Wild Overlay Zones` list to the `World Gen & Loot` config. This allows server owners/modpack makers to explicitly control which color zones the genetics engine is allowed to use when picking an overlay for a naturally spawning hamster. Defaults to natural colors (`WHITE`, `LIGHT_GRAY`, `DARK_GRAY`, and `CREAM`).
    - Added new `Restricted Base Colors` and `Clashing Overlay Colors` lists. By default, these prevent `CREAM` and `DARK_GRAY` wild overlays from spawning on top of `BLUE` and `LAVENDER` base coats, as these color combinations tend to look a bit strange visually.
    - Added new `Enforce Brighter Overlays` and `Enforce Muted Overlays` toggles. Allows players to disable the overlay filtering that prevents saturated colors (like Cream) from spawning on muted bases (like Black) and darker colors from being used as overlays on top of lighter bases.
    - Modifying the overlay settings can dramatically change the number of possible combinations for wild hamsters (3,231 by default).
  - **Red Eye Config Toggle**
    - Don't like the red eye hamsters? You can visually disable that for your own client via a new config toggle, which makes them appear to have black eyes.
  - **Friendly Fire Config Toggle**
    - Added `Prevent Owner Friendly Fire` to the Core config toggles.
    - When enabled, players can no longer accidentally damage their own tamed hamsters. Disabled by default to remain vanilla-friendly.
    - In the next update (hopefully), this setting will be tied to a new item: the **Acorn Ring**. I also plan to integrate with the **Trinkets** mod so you can use a trinket slot for it.
  - **Configurable Cuisine**
    - You can now configure the nutrition and saturation values for all food items added by the mod (Cucumbers, Green Beans, Food Mix).
    - Changed nutrition/saturation values are dynamically reflected in AppleSkin's "on-eat" HUD preview. **Unfortunately due to major API changes between MC versions, this only works on 1.21.1.*
  - **Max Mounted Hamsters Config**
    - Added a configuration slider under 'Shoulder Hamster Settings > Core Settings' allowing players to limit the maximum number of hamsters that can be mounted simultaneously (1 to 3).
- **Gameplay Mechanics & Interactions**
  - **Tag Mini-Game**
    - Hamsters can now initiate a playful game of tag. If you maintain eye contact with a hamster for a few seconds, it will squeak and excitedly run away.
    - **The Chase:** The hamster will flee if you get too close and stop to playfully taunt you if you fall too far behind.
    - **Payoff:** Successfully "catching" (right-clicking) the hamster before it gets bored ends the game triggers a celebration. The hamster will then spit out a random item from its cheek pouches as a gift (pulling from the configurable cheek pouch loot lists).
    - **Stranger Danger:** By default, you can play tag with wild hamsters and hamsters owned by other players. (Can be disabled in config).
    - **Configurable Limits and Rewards**
      - Added configs to control how often they want to play, the cooldown between games, and an anti-abuse cap on how many times a single player can play the game per in-game day (so they can't farm rewards).
      - Added configs to control rewards: By default, hamsters will randomly pick an item from their configurable "Cheek Pouch Loot" list (the list of potential items a wild hamster might spawn with). The item does not have to be in the hamster's cheek pouch for it to give it as a gift.
      - That can be disabled, in which case the hamster will choose the gift from a separate "Custom Tag Rewards" list in the config, which means you can make your hamster give you a diamond or any modded item (or whatever you want) when you catch it after a game of tag. Only server moderators can modify this of course.
  - **Dynamic Gaze**
    - Hamsters will now sustain eye contact with you indefinitely, provided you are nearby and continue looking back at them, and of course as long as they don't start a game of tag.
  - **Hamster Yeet Overhaul**
    - Thrown hamsters are now officially registered as `ProjectileEntity` instances while airborne. This provides automatic, out-of-the-box compatibility with external mods that use targets or hoops (such as the Tin Hoops in Caverns and Chasms).
    - Thrown hamsters now experience dynamic physics upon impact. They will ricochet backward off of walls and entities, or skip forward when hitting floors and ceilings.
    - Thrown hamsters now dynamically query and broadcast the native sound of the block or entity they strike (including modded blocks/entities).
    - Impact physics are magnified when hitting Slime blocks, and completely absorbed by Honey blocks.
    - Added a new config toggle (true by default) allowing thrown hamsters to damage their owner. Throwing your own rodent straight up in the air will now result in a realistic, concussive reunion on the way back down.
  - **Precision Tree Heists**
    - Right-clicking Oak Leaves with a lure item (Cheese) while a hamster is on your shoulder will now initiate a "Precision Tree Heist."
    - This sets that specific leaf block as the guaranteed exit point for the hamster.
    - While a precision heist is active, right-clicking in the air with the lure item will set the hamster's exit direction to match the direction you are currently looking.
    - **Why?** This allows you to precisely control the exact block and angle by which your hamster will exit the tree, which makes for much more predictable recording sessions.
  - **Moonwalking Easter Egg**
    - Name your hamster "Michael Jackson" or "Steve Irwin" and it will rotate backwards and remain that way until you change the name again.
  - **Expanded Default Diet**
    - Hamsters can now eat seeds from almost any mod out-of-the-box. Added a robust union tag to the default config that automatically syncs with `#c:seeds` and `#forge:seeds` to ensure compatibility across all versions and mod loaders.
- **Failsafes & Rescues**
  - **Teleport Rescue Protocol**
    - Overhauled how hamsters follow you across vast distances or dimensions (e.g., when using Waystones).
    - Instead of relying on their tiny legs and pathfinding AI to catch up, your player code will now safely scoop up any actively following hamsters (and their babies if the babies are following them), hold them in a little NBT pocket while the world loads, and drop them at your new location 0.75 seconds later.
  - **Void Rescue Protocol**
    - If a player falls into the void and dies with hamsters on their shoulders, the hamsters will no longer spawn in the void and immediately perish. The system will safely teleport them back to their linked bed. If they do not have a linked bed, they will be sent to the player's personal respawn point or the world spawn.
- **UI, HUD, & Audio**
  - **Jade Integration**
    - Added comprehensive Jade overlay integration that reveals a hamster's exact genetic makeup (base coat, wild overlay, breeding overlay, eye genotype, etc.) when you look at them.
    - Enabled without needing to turn on debug mode to help with breeding.
    - Fully customizable. Each setting comes with its own toggle, and you can decide which ones show up when you sneak to keep the Jade HUD de-cluttered.
  - **GUI Renaming System**
    - Players can now rename their hamsters directly from the Hamster Inventory screen.
    - Features dynamic down-scaling to ensure long names always fit perfectly within the UI constraints.
    - An interactive pencil icon and underline display when hovering/typing.
    - **New Config Options (`UI & Quality of Life > Hamster Renaming`)**
      - `Enable GUI Renaming`: Master toggle for the feature.
      - `Consume Name Tag`: Forces players to sacrifice a Name Tag from their inventory (or the hamster's cheeks) to finalize the rename.
      - `Pencil Icon Placement`: Allows swapping the icon to the left or right of the text for those who read in different directions.
  - **New Audio & Visuals**
    - Added a brand new `anim_hamster_cheek_unload` animation used for gifting items, complete with item and spit particle effects.
    - Added 4 new scratching sound variations for the cleaning animation, and switched to keyframes for the SFX to make it less repetitive and more realistic. This also has the side effect of muting the sound effect if the hamster is not on screen, and thus not being rendered. This means you will no longer hear nearby hamsters cleaning unless you can also see them.
    - Added dynamic item sounds (clink, squish, thud, etc.) to the new gifting sequence.
- **Commands & Admin Tools**
  - **Advanced Spawning & Testing Commands**
    - Added `/ahp spawn hamster <basePalette> <wildPattern> <wildPalette> <breedPattern> <breedPalette> <eyes>`. This command features a custom Brigadier auto-complete engine that suggests exact, human-readable palette and pattern names as you type, allowing you to easily test specific genetic combinations.
    - Added `/ahp spawn_all_bases_2D`. Systematically lines up every single hamster color base in the game (including custom community additions), sorted cleanly into rows by their dynamically determined, mathematical color zone.
    - Added `/ahp spawn_all_bases_3D`. Spawns the hamsters hovering in a physical 3D "cylinder" representing their Hue/Saturation/Brightness coordinates. This is useful for the **Breeding Inheritance and Visualization Tool** (see below).
      - **Hue** = the degree of the circle (0-360)
      - **Saturation** = the radius of the circle (more saturated colors are closer to the edge)
      - **Brightness** = the height of the cylinder (brighter hamsters are closer to the top)
      - By default, each `/ahp spawn_all_bases...` command spawns just the 45 base colors, but each one comes with 3 optional arguments to change the number of hamsters spawned:
      1. `[with_wild_overlays]` Displays all 3,231 hamster variants that can spawn naturally in the wild.
      2. `[with_sample_breeding_overlays]` Displays genetic combinations of every wild hamster but adds in a small sample of three breeding overlays (Tortoise Shell, Silver, and Rust). Due to the triple-multiplication math involved, this heavily increases the total number of hamsters spawned (from 3,231 up to 9,693) creating a much bigger display.
      3. `[author]` only uses color palletes from a certain author (e.g., `jimcerberus` or `default`).
    - Added `/ahp spawn_all_possible_permutations_THIS_CAN_BREAK_YOUR_WORLD`. Exactly what it sounds like. Don't run it unless you have a super-flat world, a fire extinguisher for your PC, and a lot of patience. Note that every time I have run this command, my server tick speed never quite returned to baseline— even after deleting all the hamsters that were spawned using `/kill @e[type=!player]`.
    - Added `/ahp print_genetics_report` command, allowing server operators to recalculate and view the current 3D color-space math of the genetics engine at runtime. Useful because it dynamically updates its readout based on your exact `AhpWorldGenConfig` wild overlay settings, so you can see how your changes are affecting the total possible variant numbers without having to spawn them all.
  - **Tree Heist History Command**
    - Added `/ahp reset_tree_economy` for quick clearing of tree depletion memory without needing to open the config screen.
- **Advancements**
  - **Advancement: Carat Confusion**
    - Triggers when a hamster leads you to Gold Ore instead of Diamond.
  - **Advancement: Load-Bearing Human**
    - Triggers when you mount 3 hamsters simultaneously (Right, Left, Head).
  - **Genetic Progression Advancements**
    - Added "The Collector" advancement path tracking unique wild variants tamed (up to dynamic maximum based on configuration limits).
    - Added "The Breeder" advancement path tracking unique bred combinations (up to 1,000,000).
    - Added a conditional advancement "Seeing Red" for successfully breeding the recessive eye trait. Does not trigger if red eyes have been turned off in the config.
    - Implemented high-performance, non-bloating NBT storage using IntArrays to track thousands of `HamsterGenome` hashes without lagging the player entity.
- **Guidebook Enhancements**
  - **Guidebook Delivery Fallback**
    - Added a new configuration option that acts as a fallback for modpacks that disable auto-guidebook delivery on login.
    - If enabled (default), players will automatically be given the Hamster Tips guidebook and receive a chat prompt the very first time they spot a wild hamster from <=10 blocks away.
    - Targeting calculation includes wiggle room to ensure the event triggers even if the player's crosshair isn't perfectly on the hamster's hit box.
    - Only triggers once, and only if the player has never yet received the guidebook (regardless of current inventory status).
  - **Lectern Reading**
    - Added code for placing the Hamster Tips guide book into a lectern and an event handler so you can read it. You can now display your rodent knowledge in your base or wherever.
  - **Hamster Tips Guidebook Improvements**
    - The *Hamster Tips* guidebook now uses a custom string processor to pull live data directly into the text, so I can do things like automatically displaying the exact number of mathematically possible wild variants or the name of your dynamically configured Lure Item instead of hardcoding "Cheese".
    - Updated the "Regional Rodents" entry to explain the new biome-adaptive color logic, Wild Overlays and Jade integration.
    - Added a new "Breeding" entry in _The Hamster Life_ chapter detailing the genetic mechanics, recessive red eyes, and feral youth mechanics.
    - Added a new "Admin Commands" entry in _The Kitchen Drawer_ chapter detailing the various new (and old) commands the mod has to offer since some of them (especially the new ones) are super useful.
- **Pixie-Dust Crown for Supporters**
  - Added a dynamic, spinning, bobbing particle crown that renders above the heads of mod supporters. Uses a custom `Pixie Dust` particle system featuring a dense, short-lived, shimmering effect.
  - Emits a subtle sparkling sound and includes 5 different themes (Gold, Crimson, Lavender, Ice, Emerald) by manipulating the HSB color values of a single grayscale texture at runtime.
  - Automatically hides in first-person view to prevent visual obstruction.
  - Powered by a remote manager that pulls configuration data asynchronously from GitHub and caches it for offline use. You will need to briefly connect to the Internet while launching Minecraft at least once to verify your ownership of the Crown. After that you can play offline and still see it!
  - **Configurable Aesthetics**
    - Added a new `Supporter Perks` config group.
    - Added a global toggle to disable rendering of all crowns if you hate fun.
    - Added a global toggle to disable sound effects of all crowns.
    - Added a local toggle to allow rendering your own crown in first-person mode if you want to both obstruct your view and thrill yourself.
    - Added sliders for sound volume, particle count, crown radius, crown height, crown thickness, and vertical offset.
    - Added a new "Toggle Supporter Crown" keybind allowing users to show/hide their own crown from both themselves and the rest of the server. A single press cycles the crown to the next available color theme, while a double-tap toggles the crown's visibility.
    - Supporters can pick their own crown color. Your preference syncs instantly to all other players looking at you utilizing `DataTracker` networking.

### Changed
- **Location-Centric Spawning Overhaul**
  - Completely rewrote the world generation spawning logic. Instead of mapping individual hamster colors to biomes, biomes are now grouped into 9 "Spawning Environments" (e.g., Icy, Sandy, Forest, etc.)
  - Each environment rolls against weighted hamster color groups. This ensures that whether a texture is procedurally generated or community-made, it mathematically evaluates its own color and automatically spawns in a biologically appropriate location.
- **Diamond Sniffing Visuals & Audio**
  - Added directional animations to indicate whether a buried diamond ore is above or below the hamster when it's sniffing for one and the path is obstructed.
  - Added a dynamic "quick bounce" animation that intermittently triggers when the diamond ore is hidden somewhere above the hamster.
  - Added a new head-shake sound effect that plays when the hamster is sniffing for ore and gets confused.
- **Sweet Potato Easter Egg**
  - The "Sweet Potato" Easter egg now applies a unique custom texture to the hamster, thanks to [**@jimcerberus**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com)!
  - Renaming a hamster "Sweet Potato" hides its normal genetics (base coat, overlays, and eye color) without permanently deleting them.
  - Sweet Potato hamsters can still breed, and their offspring will genetically inherit traits mathematically blended from the sweet potato's unique color palette, but they do not spawn in the wild.
- **Jade HUD Config Settings**
  - Added a new "Jade Overlay Settings" section to the config under "UI & Quality of Life". 
  - Displays complex genetic info (Age, Base Coat, Wild Overlay, Breeding Overlay, Eye Color) by default.
  - Added a "Require Sneaking" toggle (disabled by default) allowing you to hide the genetic overlay unless you are actively sneaking.
  - Added individual toggles for every piece of my custom genetic info shown in the Jade overlay.
  - Added additional toggles for selectively disabling default Jade info lines (Name, Health, Growth Time, Owner, Inventory) specifically for hamsters without impacting other entities, and a toggle to link their visibility to the player's sneaking state, allowing you to completely customize the HUD to your liking.
  - Built a smart-formatting engine so that community texture IDs are automatically converted into localized, human-readable titles on the HUD (e.g., `cheesecake_mocha.png` -> "Cheesecake Mocha").
- **Statue Performance & AI Toggling**
  - Hamsters spawned with their AI disabled will now completely freeze their animation playback and no longer emit ambient idle squeaks. This attempts to reduce client-side rendering lag when hundreds of thousands of hamsters are on screen for testing, but Java itself struggles with that many cubes on screen, so it's still laggy if you try to look at 2 million+ hamsters simultaneously.
  - You can now tame an AI-disabled hamster, which will instantly "wake it up" and turn it into a real, fully functional hamster.
  - Added an `Allow Taming to Re-Enable AI` toggle to the config in case you're a server owner who wants to sell specific hamster breeds in a shop. If you have OP permissions on your server, you can turn this off to prevent players from taming and "waking up" frozen hamsters, allowing them to be used as shop displays or decorative statues.
- **Mod Page/README**
  - The README was functioning as a marketing poster, a technical manual, a credits roll, and a tutorial. That is too many jobs for one file.
  - It was getting too large, so I have reorganized it, reworded it, and split a few things off into other easily accessible files, so the README only has one job now.
- **Mount Priority**
  - Changed default config value to `HEAD_FIRST`.
- **Action Bar Config Toggles**
  - Grouped all action bar message toggles (Shoulder Dismount, Tree Heist Start, Bed Break, Tamed Baby Warning, and Display Duration) into a new "Action Bar Messages" sub-category under "UI & Quality of Life" for easier access.
- **Guidebook**
  - Updated Hamster Bed entry to explicitly mention the Totem of Undying requirement for respawning.
- **Guidebook Effects**
  - If you obtain the Hamster Tips guidebook while viewing a chest or crafting screen, the "rediscovered" visual and audio effects will now be postponed until you close the screen, ensuring the effects do not play while you're looking at your inventory.
  - If you keep the screen open for longer than 5 seconds after obtaining the book, the effects are silently cancelled.
- **Cucumber Rebalance**
  - Sliced Cucumbers are no longer inexplicably nutritious. Their default food value has been dropped from 2 hearts to 0.5 hearts to match items like Dried Kelp.
- **Hamster Hitbox Adjustment**
  - Shrunk the physical hitbox of hamsters by 15% to more accurately match their actual visual model size.
  - *Note: Because the bounding box is now smaller, you will need to aim slightly more accurately when trying to interact with a hamster.*
- **Patchouli Version**
  - Updated the internal guidebook generation logic to accommodate API breaking changes introduced in Patchouli version `1.21.1-93`.
  - You must update Patchouli to the latest version or the game will not launch.
- **Shoulder Animation Configs**
  - Replaced the single `Forced Animation State` setting with three distinct settings. You can now independently force the Head, Left Shoulder, and Right Shoulder hamsters into specific animation loops (when dynamic animations are disabled).
- **Tree Heist Exits**
  - Hamsters will now perform a small outward jump, launching themselves away from the tree upon successfully completing a Tree Heist.
- **Breeding Cooldown Config**
  - The config setting for breeding cooldown has been changed from Ticks to Seconds to make it significantly easier to manage with the slider.

### Fixed
- **Config Live Changes**
  - Fixed an issue where changing settings in the `World Gen & Loot` config (such as wild bush or hamster spawning settings) required a full game restart to take effect. Saving changes to any config now recalibrates the mod's logic caches on both the client and the server.
- **Visual Rotation Glitch**
  - Attempted to fix an issue where using teleportation mods (like Waystones) or interrupting animations could cause the hamster's model to become permanently rotated backwards or upside down on the client side.
  - I can't be 100% certain about this fix yet, because no one has been able to figure out exactly what causes it and I can't re-create it myself, so let me know if you run into it.
- **Hamster Stretching with Shaders**
  - Fixed a visual bug where hamsters would stretch to extreme sizes during multiplayer server lag or when pausing the game.
- **Dismount Bug**
  - Fixed an issue where holding down the Sneak key for more than a second would cause the OS's auto-key-repeat feature to spam the game with inputs, dismounting all of your shoulder hamsters rapidly.
  - You can now safely hold Sneak without losing your friends.
  - This bug has existed ever since the double-tap setting was added, but I hadn't found it because I usually play with Sneak set to "Toggle" (which means I have no reason to hold it down for extended periods).
- **Memory Optimization**
  - Implemented pre-caching for texture identifiers and simplified rotation math in the render loop to eliminate extra memory objects being generated every frame.
- **Spanish Localization**
  - Some strings in the Hamster Tips guidebook were still outdated (i.e., the Accessories and Sunflower pages).
- **Malformed Recipe**
  - The `sliced_cucumber_from_cutting_board.json` recipe for compatibility with Farmers Delight failed to load on 1.20.1 due to a typo.
- **Acorn Inflation**
  - Fixed a decimal point error where Oak Leaves dropped Acorns at a 5% rate instead of the intended 0.5% (now it correctly matches vanilla Apple rarity).
- **Guidebook Effects**
  - Fixed an issue where moving the Hamster Tips guidebook around in your own inventory would sometimes re-trigger the "rediscovered" sound and particle effects.
  - Added a 30-second grace period to the guidebook tracking system. Prevents the effects from spamming if you move it between inventories, or drop it and pick it back up.
- **Pink Petals Bug (1.20.1 only)**
  - Fixed an issue where Pink Petals would visually apply to all three locations simultaneously when first equipping them or reloading a world.
- **Hamster Bed Linking**
  - Reduced the maximum stack size of Hamster Beds to 1. Not only does this feel like the way it always should have been, it resolves an issue where holding a stack of beds and right-clicking a hamster would link the entire stack simultaneously.
- **Invisible Hamster Glitches**
  - Solved a vanilla Minecraft issue where hamsters (and other pets) get left behind in unloaded chunks because their AI stops ticking before they can teleport.
  - Fixed a related bug where teleporting hamsters would sometimes successfully arrive but remain completely invisible to the client until you relogged. The new Teleport Rescue Protocol bypasses these vanilla quirks entirely.
  - Overhauled shoulder-hamster data synchronization to prevent them from becoming invisible upon player respawn if they were configured to respawn with the player when the player fell into the void.
- **Ghost Bed Crash**
  - Fixed a server crash that occurred when a hamster's linked bed was destroyed while the hamster was sleeping or unloaded. Hamsters will now detect the missing bed, cancel their sleep effects, and unlink themselves to prevent future issues.
- **Bed State**
  - Fixed an issue where chunk load order could occasionally cause a sleeping hamster's bed to visually revert to an "unoccupied" state across server restarts.
- **Shoulder Hamster Physics**
  - Capped the maximum vertical offset in the shoulder hamster physics simulation. This prevents hamsters from visually floating too far off the player's shoulders during long, extreme falls.
  - This was mostly an issue with resource packs that add cool player animations to the arms. The shoulder hamsters are locked to the arms of the player, so when the arms go out to the sides during a fall, the shoulder hamsters would cross over the midpoint of the head. This doesn't fix the issue 100% (I'm not sure if that's even possible with pretty player animations) but makes it a bit less obvious.
- **Shoulder Cleaning Loop**
  - Fixed a bug where a hamster mounted to the player's shoulder mid-cleaning would get permanently stuck in the cleaning animation when it was supposed to be sitting while on the shoulder.
- **Jade Debug Overlay**
  - Fixed an issue where the Jade debug toggle book-interaction failed to update the client config on dedicated servers.
- **Non-Dynamic Taming Food**
  - Fixed a bug where wild hamsters would still flee from players attempting to tame them using anything other than Sliced Cucumber. Now correctly uses the configurable "Taming Foods" from the config.
- **Suicidal Hamsters**
  - Hamsters will no longer choose to run through lava or fire when cornered and fleeing from a player. They will now properly recognize these hazards as completely impassable terrain.
- **Immersive Engineering Compatibility**
  - Fixed a data formatting error in the Garden Cloche recipes for cucumbers and green beans that caused the game to crash upon world creation or loading. (Thanks to [@CasualAnimalEnjoyer](https://github.com/CasualAnimalEnjoyer) for the fix!)

---

## [3.5.0] - 2025-02-08

# The Tactical Fluff & Tree Heist Update

The hamsters were too squishy. This update introduces enchantable armor, a new accessory, and configurable bed respawning. I may have also added a questionable, overengineered method of deforestation, configurable loot injection, farming mechanics, and quality-of-life features while I was at it... and TONS of other stuff. It’s a big one. Brace yourself.

### Added
- **Hamster Armor System**
  - **Acorn Armor:** The biodegradable base tier. Crafted from Acorn Shards and an Acorn Hat. It works like Wolf Armor— completely negating damage until it breaks.
  - **Tiered Upgrades:** Use the Smithing Table to plate your Acorn Armor with Iron, Gold, Diamond, or Netherite using new **Smithing Templates** found in places where normal smithing templates are usually found.
  - **Tactical Perks (Configurable):**
    - **Iron:** Reduces wind resistance, granting extra flight velocity.
    - **Gold:** Lightweight; grants extra movement speed.
    - **Diamond:** Shiny; cures kleptomania. Hamsters wearing diamond armor will **fetch** valuables instead of stealing them.
    - **Netherite:** Dense; grants increased throw inertia and knockback resistance.
  - **Enchantment Support**
    -   Hamster Armor can accept **Frost Walker**, **Fire Protection**, **Soul Speed**, **Mending**, and **Unbreaking**.
    -   *Note: Protection, Thorns and Feather Falling are intentionally excluded because hamsters don't take fall damage and Hamster Armor negates 100% of incoming damage until it breaks.*
    -   Got any ideas for other enchantments that should be supported? Let me know on **[Discord](https://discord.gg/w54mk5bqdf)**.
- **Tree Heist Mechanic**
  - **The Trigger:** Throw your hamster at an Oak Tree (or dismount them while staring at Oak Leaves) to initiate a heist.
  - **The Simulation Architecture:**
    - **Intelligent Canopy Mapping Algorithm:** Upon impact, a Gradient Descent Scan locates the trunk anchor. Then a Breadth-First Search utilizes leaf distance gradients to intelligently segment overlapping foliage, allowing the system to isolate and map the specific 3D shape of a single tree, even within a dense forest where the leaves are all touching.
    - **Proxy Entity:** Your physical hamster is temporarily swapped for an invisible, weightless `TreeSearcher` entity. This proxy physically navigates the mapped leaf volume for that specific tree in real-time, randomizing its path to "rummage" for acorns.
    - **Spatial Integrity:** The system verifies the tree's structural integrity every second. If you chop the tree down while a heist is active, the proxy detects the destruction, forces an emergency eject, and your hamster exits.
  - **Visual & Audio Fidelity:**
    - **Reactive Foliage:** A deterministically randomized physics simulation makes the leaves physically jiggle and shudder as the proxy moves through them. Each block has a unique oscillation pattern derived from its coordinates, further selling the "scurrying" effect.
    - **Dynamic Audio:** Sound sources track the invisible proxy's position, meaning you can hear exactly where your hamster is inside the canopy.
  - **The Economy:** Trees possess persistent "profitability" memory. Over-farming the same coordinates depletes yields until your hamster finds nothing and pouts.
- **Lost Hamster Rescue Protocol**
  - Added a new quality-of-life feature for wander mode. If a hamster is unable to find a path back to its bed while wandering, simply go to sleep in your own bed.
  - When you wake up, any of your stuck hamsters will have "found their way" (teleported) back to their beds and will be sound asleep.
  - Requires the hamster to be loaded and the bed to be unoccupied.
- **Configurable Action Bar Duration**
  - Added a new setting in "UI & Quality of Life" to adjust how long action bar messages stay on screen so now you can actually read them. I couldn't find a mod that did this so I just added it myself. Defaults to 5 seconds (vanilla is 3 seconds), adjustable up to 15 seconds.
- **Hamster Tips Guide Book to Custom Creative Tab**
  - Registered the guide book item in the Adorable Hamster Pets creative menu group.
  - Linked my custom Advancements tree to the Guidebook with a star-shaped button on the landing page.
- **Hamster Tips Guidebook Chapters**
  - **New "Acorn Armor" Entry:** Explains how to throw your pet at a tree for profit, and how to forge nuts into armor.
  - **New Category/Chapter:** "Loot & Scavenging" – detailing the new loot locations and configurability.
- **Acorn Hat Accessory**
  - Increases Tree Heist profitability by 2x, yielding more acorns. Fashionable and functional.
- **Acorn Shards Ingredient**
  - Processed Acorns used to construct the base armor.
- **New Inventory Slots**
  - Added dedicated **"Bling"** and **"Armor"** slots with interactive UI sounds.
- **10 Advancements**
  - "Tree Heist" (Start a Tree Heist)
  - "Return on Investnut" (Obtain an Acorn)
  - "Ecological Menace" (Exaust a tree's supply)
  - "Industrial Nutcracking" (Obtain Acorn Shards).
  - "Hardened Salad" (Craft Acorn Armor)
  - "Squirrel Cosplay" (Equip Acorn Hat)
  - "Expensive Therapy" (Craft Diamond-Plated Acorn Armor)
  - "The Immovable Object" (Craft Netherite-Plated Acorn Armor)
  - "MURDERER" (Kill a hamster)
  - "Rodent Reckoning" (Be killed by a hamster)
- **Hamster Bed Respawning**
  - Added a configuration toggle (`enableRespawnInBed`) to allow hamsters linked to a bed to respawn there upon death. Disabled by default so fit the mod's vanilla-friendly theme.
  - Respawning hamsters retain their inventory, name, and owner. They may not retain other attributes added by external mods.
  - Added tooltips to the Hamster Bed item and Jade overlay indicating if respawn is active.
  - Added a new page to the Hamster Bed entry in the Hamster Tips guidebook explaining the feature.
- **Dynamic Aerodynamics**
  - Hamsters now react to gravity with appropriate drama. The "Flying" animation and dynamic nose-dive rotation logic are no longer exclusive to the Hamster Yeet.
  - Any significant fall— whether it's jumping off a ledge or popping out of a tree— will now automatically trigger the flight pose and dynamic pitch rotation (uses smooth cosine interpolation), ensuring they always look like they are falling.
- **Global Loot Table Injection**
  - **Common Loot**
    - **Items:** Cucumber, Green Bean, and Sunflower **Seeds**.
    - **Locations:** Villages (Houses, Butchers, Shepherds), Dungeons, Mineshafts, Shipwrecks, Outposts, Small Underwater Ruins, Trial Chamber Supply chests.
  - **Uncommon Loot**
    - **Items:** Standard Armor (Acorn, Iron, Gold).
    - **Locations:** Desert Pyramids, Jungle Temples, Igloos, Ruined Portals, Village Blacksmiths, Large Underwater Ruins.
  - **High-Tier Loot**
    - **Items:** Diamond Armor and Basic Upgrade Templates (Iron, Gold). (Also Netherite Armor, if enabled in config).
    - **Locations:** Nether Fortresses, Bastions, Stronghold Corridors, End Cities, Ancient Cities, Trial Chamber Reward chests.
  - **Legendary Loot**
    - **Items:** Accessories (just the Acorn Hat for now) and Advanced Upgrade Templates (Diamond, Netherite).
    - **Locations:** Woodland Mansions, Buried Treasure, Stronghold Libraries, Ominous Trial Spawners.
  - **Configuration:**
    - Check the **World Gen & Loot** config to adjust the drop chance for every single category listed above, or disable them entirely. Requires a restart to take effect, which is why I put it in the World Gen & Loot config so you can modify it before loading into the world.
- **Wild Cheek Pouch Loot**
  - Wild hamsters now have a chance (Default 50%, Configurable) to spawn with scavenged items (seeds, nuggets, etc.) in their cheek pouches.
  - **Context-Aware Scavenging**: Hamsters found in caves have a unique loot pool containing raw ores, glowing berries, and other subterranean treasures. I did not add Diamonds to the list, but you can certainly do that yourself in the config.
  - **Lopsided Cheeks**: There is a 60% chance the loot will spawn in only one cheek, creating a cute, asymmetrical look.
  - **Logic**: These items drop on death (also configurable), or persist when tamed— meaning if you tame a wild hamster with full cheeks, you get to keep the loot once you unlock the pouch.
  - **Configuration**
    - The **World Gen & Loot** config includes an "Extra Loot" list. You can define exactly what items wild hamsters spawn with (even items from other mods) and tweak their rarity separately from the default loot list, which is also configurable. Requires a restart to take effect.
- **Sunflower Farming**
  - **Plantable Seeds**: `Sunflower Seeds` can now be planted to grow the custom 2-block tall Sunflower.
  - **Bonemeal Duplication**: Right-clicking a fully grown custom Sunflower with Bonemeal now drops a vanilla Sunflower item, matching vanilla tall-flower behavior.
- **Head Mount Priority**
  - Added a "Mount Priority" setting (`Shoulders First` vs `Head First`).
  - You can now configure hamsters to prioritize the **Head** slot before filling the shoulders, allowing a single hamster to be mounted on the head. It's kind of my new favorite thing to do. I'm even considering making it the default lol. Let me know your thoughts in the **[Discord server](https://discord.gg/w54mk5bqdf)**!
- **Gust Volume**
  - Added a volume slider for the "Gentle Breeze" sound effect used by Hamster Bedding particles. In some modded environments with mods like **Sound Physics Remastered**, the gust SFX was not loud enough to be audible.
- **Configurable Ore Seeking**
  - Added new `celebrationOres` (Desirable Ores) and `sulkingOres` (Disappointing Ores) lists to the config.
  - You can now define exactly which blocks your hamster gets excited about (or disappointed by) using Block IDs or Tags.
  - This affects both the Shoulder Hamster Alert and the Independent Seeking behavior.
- **Sitting Headshake**
  - Added `anim_hamster_sitting_headshake` for when a hamster refuses food while sitting.
  - The food refusal logic now intelligently selects between sitting, standing, and moving headshake animations.
- **Missing Guidebook Warning**
  - Added a client-side check that runs 3 minutes after joining a world (configurable).
  - If the player is missing the guidebook, a dramatic, clickable chat message appears, offering to open the config screen to reclaim it.
  - Includes a solemn oath to read the manual before asking questions in Discord.
  - It cannot be disabled. Too many players have come to the Discord asking questions that are answered directly in the guidebook. Upon further investigation, I find they did not get the guidebook and didn't know about it, because the creator of their modpack turned off auto-delivery.
  - This warning persists across servers/worlds so it only triggers once per person on single player, and once per person per server on multiplayer (unless reset in config).
- **Configurable Wander Interval**
  -   Added a slider to the Config to control the probability of a hamster deciding to wander.
  -   Setting this to **0** completely disables idle wandering, useful for keeping them stationary for photos or specific builds.
  -   For now, this will affect all hamsters. I'll add a specific photo shoot feature later.
- **Configurable Look At Player Duration**
  -   Added a "Look-At Duration" slider to the config.
  -   Controls how long hamsters stare at you before getting distracted.
  -   **The Math:** Actual duration = `Config Value` + `Random(0 to 4 seconds)`.
  -   Example: If set to 60 ticks (3s), they will look for anywhere between 3 and 7 seconds.
- **Hamster Riding (Commissioned by [@Saint_Victus](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com))**
  - Added a new configuration option: "Enable Hamster Riding" under a new "Commissioned Features" category.
  - Adds a keybind (`Ride Hamster`, unbound by default) that allows you to mount a hamster.
  - **Dynamic Rider Visuals**
    - I hooked into the Geckolib rendering pipeline so players riding a hamster physically attach to the `body_child` bone.
    - This means riders will bob, sway, and rotate in perfect sync with the hamster's running animations instead of floating statically on top.
  - **Mechanics**
    - You can ride *any* hamster (even wild ones), but you can only _control_ hamsters you own (includes jumping!)
    - Riding an unowned hamster results in an uncontrolled ride (like a pig without a carrot).
    - Riding your own hamster disables its Wander Mode to give you full control.
  - **Unofficial capacity**
    - This commissioned feature doesn't fit the theme of the mod, so this changelog is the only place you'll find it mentioned.
- **Legacy Forge Support** (Thanks to [@Konkeeztador](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com)!)
  - Added data structures and loading conditions to ensure the mod tags/recipes load correctly on the legacy Forge loader.
- **Mod Compatibility Improvements** (Thanks to [@CasualAnimalEnjoyer](https://github.com/CasualAnimalEnjoyer)!)
  - Added conventional `c` tags for Cheese to improve cross-mod compatibility.
- **Supplementaries Compatibility**
  - Hamsters can now be caught in Cages from the [**Supplementaries**](https://modrinth.com/mod/supplementaries) mod. (Thanks to [**@just_a_cricket**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com)  for the built-in datapack!)
  - To enable catching of wild hamsters, go to `config > supplementaries-common.json > functional > cage > require_taming` and turn it off.
- **Botany Pots Compatibility**
  - Added support for [**Botany Pots**](https://modrinth.com/mod/botany-pots). You can now grow Cucumbers, Green Beans, Sunflowers, and the Wild Bushes in pots! (Thanks to [**@CasualAnimalEnjoyer**](https://github.com/CasualAnimalEnjoyer) for the built-in datapack!)
  - Hamster Bedding can also be used as a fertilizer in the pots.
- **Configurable Biome Tag Exclusion:**
  - Added a new `Exclude Biome Tags` list to the World Gen config. By default, this now excludes `#minecraft:is_ocean` and `#minecraft:is_river` to prevent spawns in those biomes regardless of other settings.
- **Glowing Sunflowers** (Easter Egg)
  - Added a rare event where my custom sunflowers will emit light at night, accompanied by magical particles. Rare enough that it is highly unlikely for it to affect multiple sunflowers simultaneously.
  - Includes a hidden advancement ("It Can't Be True!") for players who witness the phenomenon up close.
  - Configurable via the 'Sunflower Settings' in the World Gen config.
  - This is a reference to the Kikoriki cartoon, '[**It Can't Be True**](https://youtu.be/ztwAY6308zY?t=201)' episode, around the 3:22 mark. This feature wasn't really on my agenda, but [**@CasualAnimalEnjoyer**](https://github.com/CasualAnimalEnjoyer) requested it and I added it as a personal thank-you to them for their massive help with mod compatibility and bug fixing.

### Changed
- **Animations**
  - Updated the crash animation to make it cuter. Now instead of landing flat, the hamster bounces multiple times on its face. Includes sound effects!
- **Geckolib Version** ← **UPDATE OR YOUR GAME WILL NOT LAUNCH**
  - Updated required Geckolib version from 4.7.3 to 4.8.3 (it was way out of date) to fix animation flickering issues. It may also fix other issues that I had not come across yet— it was literally 10 versions behind lol
- **Diamond Stealing AI**
  - Refactored the diamond stealing behavior system. Hamsters now intelligently switch between "Theft Mode" (Taunting) and "Delivery Mode" (Presenting) based on the item type and their current equipment. (Configurable)
- **Refusal Interactions**
  - Attempting to open a locked cheek pouch now triggers the same new intelligent headshake animations (sitting/standing/moving) used for food refusal.
- **Item Stealing**
  - Hamsters now use their sprinting animation/speed (1.5D) when running towards a diamond to steal it, matching their flee speed. Originally I had them walking at normal speed so as not to alert the player that they were about to steal the diamond, but I changed my mind and I think it's a lot cuter if they sprint over to it.
- **Yeet Physics**
  - Thrown hamsters now apply **Knockback** to the entities they hit. The force is calculated based on the hamster's velocity, so faster throws = harder hits.
- **Pink Petal Accessories**
  - Updated rendering to use **3D models** instead of flat texture overlays.
  - Petals now have depth and similar positioning on the hamster's head, side, and back.
- **Audio Effects**
  - Added support for the `hamster_thump_sound` keyframe in the crash animation, triggering `ModSounds.HAMSTER_THUMP` for extra dramatic effect.
- **Wild Bush Textures**
  - Thanks to [**@jimcerberus**](https://this_person_did_not_want_to_include_a_link_but_I_wanted_their_name_to_be_blue.com) for giving the textures for Wild Green Bean and Wild Cucumber bushes a fresh new look!
- **Dynamic Bed Tooltips**
  - The Hamster Bed item and Jade tooltips now dynamically display the names of the configured Lure and Repellent items, ensuring the text matches your config settings.
- **Hamster Spawning**:
  - Black hamsters now spawn in Lush Caves and Dripstone Caves.
- **Update Notifications**
  - Migrated the update notification system to pull from the primary source repository, deprecating the legacy public asset repo.

### Fixed
- **Biome Spawning Logic**
  - Refactored the internal configuration to use custom union tags instead of raw convention tags (e.g., `adorablehamsterpets:is_cave` instead of `c:is_cave`). I had already made my own internal union tags months ago (which point to the Fabric's convention tags for 1.21.1 and Forge tags for 1.20.1), but forgotten to actually point to them in the config.
  - This fixes a long-standing issue where some hamster variants were not spawning correctly in certain biomes, despite the config saying otherwise. I discovered this when I noticed black hamsters were not spawning in caves, but there were probably other spawn variant issues as well which will now be resolved.
- **Guidebook Entry Index Overflow**
  - Refactored my custom text-wrapping and pagination mixin logic in the Hamster Tips guidebook index.
  - Entry titles in the Entry Index now correctly flow into new pages instead of overflowing off the bottom of the book interface.
- **Ore Seeking Logic**
  - Fixed a bug where hamsters ignored buried "Disappointing Ores" (Gold) even when configured to make mistakes, due to checking for exposed blocks instead of hidden ones.
- **Shoulder Hamster "Mutant" Splitting Glitch**
  - Fixed a visual bug on Forge/NeoForge (caused by Oculus/Iris) where shoulder hamsters would visually "split" into two overlapping models causing a z-fighting flicker effect.
  - This was caused by the animation controller randomly switching between the two idle animations every frame. They now deterministically pick one based on their personality ID.
- **Cheek Pouch Refusal Animation**
  - Fixed a bug where the "No" headshake animation wouldn't play when trying to open a locked cheek pouch. (Caused by a mismatch between the animation controller name `stationary_headshake` and the code calling `standing_headshake`).
  - The refusal logic now intelligently selects between sitting, standing, and moving headshakes.
- **World Gen Config**
  - Some of the groups had "collapsedByDefault" set to "false" which was making it messy.
- **Config Issues**
    - Fixed a few mistakes in the "Falling Leaf Settings > Static Drift Angle" config tooltip. It now correctly assigns degrees to angles.
    - Fixed a bug where configuration options (like "Top Left", "Near", "Single-Press") appeared untranslated in the config screen.
- **Sunflower Regrowth**
  - Drastically reduced the default time it takes for sunflowers to regrow seeds (Was ~2.8 hours, now ~10 minutes). My original idea was for it to feel like each sunflower gives you one set of seeds, and that's definitely how it felt until now. This change will put it more in line with the rest of Minecraft farming.
- **Inventory Label Alignment**
  - Updated the Hamster Inventory screen to dynamically center text labels ("Left/Right Cheek", "Bling", "Armor") over their respective slots.
  - This ensures proper alignment for languages with text widths different from English.
- **Missing Localization**
  - Updated Jade HUD tooltips for Hamster Beds to use translatable text for the various boolean states, removing hardcoded English "ENABLED" and "DISABLED" strings.
- **Pale Oak Bed Particles**
  - Fixed the Pale Oak Hamster Bed missing its block model, which caused missing texture (purple/black) particles when breaking the block. This ensures full visual compatibility if you are using a Pale Garden backport mod on 1.21.1 or 1.20.1.
  - Note: there is no Pale Oak Hamster Bed recipe unless you add one with a datapack. I'm running my Data Generator in a 1.21.1 development environment, and the class `Items.PALE_OAK_PLANKS` does not exist in the code yet, so I cannot reference it in the Java generator to create the recipe.
- **Fixed Water/Void Spawning:**
  - Resolved an issue where hamsters could spawn floating in oceans/rivers or over the void in Skyblock worlds. This was caused by the spawn restriction rules not being registered at the correct time in the mod loading lifecycle, effectively disabling the "valid ground block" check.
- **Data Structure & Recipes** (Thanks again [@CasualAnimalEnjoyer](https://github.com/CasualAnimalEnjoyer)!)
  - Duplicated tag folders to resolve structural differences between 1.20.1 (plural directories) and 1.21.1 (singular directories).
  - Corrected the Cloche and Insolator compatibility recipe paths for 1.20.1.
  - Fixed load conditions for Thermal Expansion integration to ensure recipes only load when the mod is present.
- **Suffocation**:
  - Hamsters now automatically trigger their "self-rescue" teleport logic whenever they start taking suffocation damage, regardless of the cause.
  - This resolves issues where hamsters would occasionally take suffocation damage while pouncing on an item. It became more of a problem once the retrieval feature was added since the hamster does not run away from the player.
- **Sleeping Hamster Pushability**
  -   Fixed an issue where hamsters sleeping in beds would become pushable after closing and reopening the world.
- **Texture Mipmapping Issue**
  - Resized Hamster Bed textures from 35x69 to 64x128 while maintaining their original look. This resolves a rendering issue where non-power-of-two textures forced Minecraft to disable mipmapping, causing all blocks in the game to appear grainy.
- **Safe Dismounting**
  - Hamsters will no longer agree to dismount directly into lava or water, even if there is a solid block underneath. The safety check now correctly verifies that the space the hamster will occupy is free of hazards.
- Fixed **Biome Detection** (1.20.1)
  - Updated internal biome tags to match Minecraft 1.20.1 standards (e.g., `c:plains` instead of `c:is_plains`).
  - This resolves issues where hamsters spawned in incorrect biomes (e.g., Gray hamsters in Deserts, Chocolate hamsters in Sunflower Plains) because the game didn't recognize those biomes correctly.
- **Invisible Shoulder Hamsters** (1.20.1)
  - Fixed an issue where shoulder-mounted hamsters would vanish after traveling to another dimension. (They still existed but only became visible after re-logging).
  - Added a forced data sync (only necessary on 1.20.1) to ensure the client immediately recognizes your shoulder hamsters after your player is re-created.
- **Item Rendering Visual Glitch** (1.20.1)
  - Fixed a visual glitch on 1.20.1 where the hamster's body would turn black when holding an item (e.g., fetching an acorn) by moving item rendering to the post-render phase.

---

## [3.4.3] - 2025-12-15

# **The Slap My Forehead Patch**

⚠️ NOTE: If you are playing on 1.21.1, you do not technically need this, unless you're seeing dozens of hamsters on your shoulder. Go have a snack.

### Fixed

#### 1.20.1
- **Server Crash: "Unknown Message Type" (The 'I misunderstood how old Architectury works' bug)**
  -   **The Issue:** On dedicated 1.20.1 servers, the game would crash immediately upon trying to sync hamster data.
  -   **The Cause:** I (sort of) copy-pasted the shiny, modern Architectury networking code from 1.21.1 into the 1.20.1 backport. Turns out, the older Architectury API requires you to introduce packets to the server politely before sending them. The server received the hamster data, said "I don't know her," and panicked.
  -   **The Fix:** Refactored the network registration to actually work on 1.20.1.
  -   Also, I have left several aggressively capitalized comments in the source code warning my future self that 1.20.1 Architectury networking is different. So hopefully this will never happen again!
#### 1.21.1 and 1.20.1
- **"Mutant Hamster Pile" Cleanup**
  -   **The Issue:** Players affected by the previous Invalid Player Data bug might have ended up with "ghost" hamster data, resulting in dozens of duplicate hamsters appearing on their shoulder.
  -   **The Fix:** Spamming your dismount button should fix the issue, but not everyone reads changelogs so I added an auto-sanitizer that runs on login. It will instantly detect and delete any ghost/duplicate hamster entries, returning your character to normal without you needing to do anything.

---

## [3.4.2] - 2025-12-11

# **The Tiny Patch**

This patch is hamster-sized, but fixes a pretty big issue. In my zeal to make my code more defensive against Macaw's Mods on Sunday, I accidentally created a bug that locked players out of worlds if they logged out with a shoulder hamster. That is now fixed. Your worlds are safe, and you can log in again. I also squeezed in Xaero's Minimap support and a few other things while I was at it.

### Added
- **New Translations**
  - Added 6 new locals for the Spanish translation thanks to [@The Retro Stitcher](https://theretrostitcher.com/).
- **Xaero's Minimap Compatibility**
  - Thanks to help from [@Kazerio](https://modrinth.com/user/kazerio), added hamster icons to Xaero's Minimap!

### Fixed
- **Invalid Player Data / World Join Crash**
  -   Resolved a critical issue where the game would crash or lock players out of worlds if they had a hamster on their shoulder. This was accidentally introduced in version 3.4.1 when I refactored the code to defend against the network protocol error caused by Macaw's Mods.
  -   I had accidentally introduced a race condition where the mod attempted to sync pet data before the player's network connection was fully established. If you had previously been locked out of any of your worlds due to this bug, you should now be able to join just fine and your hamsters will still be on your shoulder.
- **Config Screen Localization**
  -   Implemented a work-around for an issue where the "Main Settings" and "World Gen Settings" descriptions were not translatable due to a known bug with Fzzy Config.
  -   Fixed untranslated enum values (e.g., "Near/Medium/Far", "Sneak Key") in dropdown menus.
  -   Fixed untranslated conditional help text (e.g., "Only available when...").
- **EMI Compatibility**
  - Resolved tooltip mod name duplication when EMI is installed.

---

## [3.4.1] - 2025-12-07

# **The Edge Case Extravaganza Patch**

This patch is dedicated to the 1% of you running modpacks complicated enough to break the space-time continuum. If you weren't crashing, you probably won't notice a difference. If you *were* crashing because you threw a hamster at a target more than 16 blocks away while running Shaders and Macaw's Furniture on NeoForge... your problems are solved!

### Added
- **Translations**
  - Updated langage files with a few translated strings that Crowdin elected to omit from the last version!
  - Major overhaul to the Russian translation of the Hamster Tips guidebook to ensure the text fits better on the pages, thanks to [@CasualAnimalEnjoyer](https://github.com/CasualAnimalEnjoyer)!

### Changed
- **Mod Icon**
  - As all true geniuses do, [@The Retro Stitcher](https://theretrostitcher.com/) woke up in a cold sweat with ideas for improving the mod's icon even further. JK about the cold sweat part. His improvements have been implemented. Thanks again!
- **The mod's `README.md` file (also known as the "landing page") has had a major overhaul!**
  - Used the Flashback mod to render epic GIFs showing off almost every feature.
  - Added new graphic headings with cozy pixel art tree branches, featuring the actual Hamster Bedding item texture for their leaves.
  - Reworked info banners for layout clarity and added credits for everyone who has helped out with development, artwork, and translations.
  - Added a new pixel art banner image up at the top. Huge thanks to [@The Retro Stitcher](https://theretrostitcher.com/) for helping me get it pixel-perfect. He and I both spent a considerable number of hours on it!
  - Go check it out [**right here**](https://modrinth.com/mod/adorable-hamster-pets), and **turn off your ad blocker** and watch a few ads while you're there to help support the development of Adorable Hamster Pets!

### Fixed

(Note: If I sent you a preview jar file on Discord marked `v3.5.0`, those fixes are included here.)
- **Added defensive measures for crashes with Shaders and optimization mods.**
  -   While not directly caused by Adorable Hamster Pets, shaders often create "fake" player entities to render shadows and aggressive optimization mods sometimes leave these entities in a broken/uninitialized state. Then when this mod would ask, "Does this shadow have a pet?" for the shoulder hamster feature, the game would panic and crash.
  -   Added defensive checks to both the **Renderer** and the **Physics Simulation** for shoulder hamsters to safely ignore these corrupted entities. This should not have any visual effect on the game except that your hamster may not have a shadow for a few seconds when you first load into the world.
- **Fixed a crash when managing Announcements from the Title Screen.**
  -   Clicking "Mark as Read" or "Snooze" while on the main menu no longer causes a `NullPointerException`.
- **Fixed stray semi-transparent pixels on the Hamster Bedding texture.**
- **Fixed localization and text errors.**
  -   Corrected the "Icon Position Preset" buttons in the config menu to show translated text instead of raw enum names.
  -   Fixed a few other grammatical errors.
- **Network Protocol Error (Macaw's Mods Conflict)**
  -   Refactored the shoulder pet data system to use custom network packets instead of vanilla DataTrackers.
  -   This eliminates ID collisions with mods like Macaw's Furniture that inject their own data into the player entity.
- **"Failed to encode packet" Disconnects**
  -   Fixed a race condition where the item stack used for particle effects was emptied before the network packet could be sent.
  -   This prevents players from being kicked when feeding/mounting hamsters cheese with a stack size of 1.
- **Crash when throwing hamsters at targets further than 16 blocks away on NeoForge**
  -   Resolved a `NoSuchMethodError` caused by a mapping mismatch when sending vanilla sound packets by migrating the "distant impact" sound logic to a custom network packet to ensure stability across loaders.
  -   Minecraft cuts off any sound that happens more than 16 blocks away from your player— this logic exists in order for players to hear those distant impacts.

---

## [3.4.0] - 2025-11-13

# **The Cuteness Overhaul Update**

## Beds, wander mode, new animations, suspiciously advanced leaf physics, and more!

Give your tamed hamsters an actual home with the new Hamster Bed, Wander Mode, and cozy bedding that doubles as a tiny leaf particle generator. Yeeted hamsters are now cuter, more terrifying, and broadcast their sound effects farther, so your friends can fully appreciate incoming fur missiles. Added new animations, updated some existing ones. Under the hood, variant spawning is now fully biome-configurable and a pile of long-suffering bugs—ghost shoulder hamsters, startup crashes, and more—have finally been escorted off the premises.

### Added
- **Fully Configurable Biome-Based Variant Spawning System**
  -   The logic determining which hamster color variants spawn in which biomes is no longer hardcoded.
  -   It is now entirely driven by new settings in the config file under `Spawn Settings > Variant Spawning by Biome`.
  -   Users can now define custom lists of biome IDs, biome tags (including `c:` convention tags), and exclusion lists for each of the seven main color variants/biome groups (Blue, Lavender, White, Gray, Black, Cream, and Chocolate).
  -   This provides significantly greater compatibility with world-generation mods like Terralith and Biomes O' Plenty, allowing users to fine-tune hamster diversity in any modded environment. The default values have been set to preserve the existing spawning behavior that was present already.
  -   The Orange variant acts as the default fallback and will spawn in any biome where hamsters are allowed that does not meet the criteria for the other configured colors. That's why it does not have its own set of biome lists in the config.
- **Hamster Yeet Audio Overhaul & Visual Enhancement**
  -   Completely re-worked the `hamster_flying` animation. It's now much cuter. Not that you'll ever see it, unless of course you record yourself throwing the hamster with the Flashback mod. Hint hint. It's quite fun.
  -   Re-designed the `hamster_throw` sound so you can hear the hamster's ears flapping in the wind, which is part of the updated animation.
  -   The throw sound now plays directly at the thrower's location, ensuring the full baked-in Doppler effect is heard instead of cutting off abruptly as the hamster flies outside Minecraft's 16-block sound attenuation cutoff.
  -   Added a new predictive "incoming" sound effect (reverse Doppler) that plays at the target's location, giving victims a one-second audible warning before impact.
  -   Thrown hamsters now dynamically rotate their pitch to match their flight trajectory (nosing up when ascending, down when falling) instead of remaining perfectly horizontal like a frisbee.
  -   Implemented a custom sound broadcasting system for long-range throws. Players between 16 and 50 blocks away (who would normally hear silence due to vanilla limits) will now hear a faint impact sound, which dynamically adjusts its volume and changes type depending on whether the hamster impacted a block or an entity. Similar to the old logic, but now you'll actually hear something if you're trying to throw a 50-yard pass.
- **New Block: The Hamster Bed**
  -   Introducing the Hamster Bed, a new block that serves as an anchor point for the new feature, **Tamed Wander Mode**.
  -   Craftable with a new "Hamster Bedding" item and wood planks. It comes in all nine vanilla wood variants (Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, and Bamboo).
  -   A tamed hamster can be linked to a bed by right-clicking it with the bed item in hand. The bed item will then store the hamster's data. Placing the linked bed in the world activates Wander Mode for that hamster.
  -   Placing the bed while targeting the underside of a block places it upside-down, triggers a new advancement, disables the bed's sleep function until it is broken, and begins to spawn floaty, cozy leaf particles. More on that in a sec.
- **New Item: Hamster Bedding**
  -   Added "Hamster Bedding," a new crafting component for the Hamster Bed, made from a cozy blend of leaves, podzol, and dead bushes. _(The leaves will be replaced with **Leaf Litter** when I port the mod to 1.21.5, giving them a fun purpose)._
  -   Beyond its use in crafting, this item can be used by hand or from a Dispenser to release a decorative cloud of leaf particles, perfect for adding a touch of autumnal ambiance to your life.
- **Advanced Particle Physics System for Bedding**
  -   The new leaf particles are driven by a custom client-side physics simulation with two distinct behavioral models.
  -   Particles spawned from bed interactions use a standard gravity-and-friction model for a simple settling effect.
  -   Particles spawned from an upside down Hamster Bed, the Hamster Bedding item or a dispenser use a "floaty" physics model, featuring a gentle pendulum-like sway based on sinusoidal motion.
  -   This floaty mode includes a deterministic, spatially-coherent wind gust simulation. It uses a grid-based hashing function to ensure particles in the same area react to the same pseudo-random wind events, creating synchronized, emergent behavior.
- **New Intelligent Indoor/Outdoor Detection Algorithm for Particles**
  -   Implemented a custom algorithm to prevent the wind gusts from the particle system from having an effect on particles that are "indoors."
  -   The system analyzes three environmental factors in real-time: skylight levels, vertical roof clearance, and horizontal openness (i.e., proximity to open doorways or windows).
  -   It uses a high-performance caching system to minimize performance impact and employs hysteresis to prevent visual flickering when particles are near the threshold between an indoor and outdoor space.
- **New Feature: Tamed Wander Mode**
  -   Tamed hamsters can now be set to "Wander Mode," allowing them to roam freely within a configurable radius of their linked bed instead of constantly following the player.
  -   **Activation:** Link a Hamster Bed item to a hamster, then place it in the world.
  -   **Control:** Right-click the placed bed to toggle Wander Mode on or off for the linked hamster. Sneak + right-click to cycle through three wander distances (Near, Medium, Far), which are configurable.
  -   Hamsters in Wander Mode will automatically seek out their bed to sleep based on the time of day (or the "Circadian Chaos" random timer) if enabled in the config.
- **Added "The Great Escape" Guide Book Entry:**
  - Details the crafting and use of the new Hamster Bed and Hamster Bedding items, featuring custom artwork.
  - Explains how to link a bed to a hamster to enable "Tamed Wander Mode."
  - Outlines all interactions for configuring wander distance, luring to bed, and unlinking.
- **New Animations and Sounds for Sit/Stand/Sleep**
  -   Added new animations for sitting down, standing up, and waking from sleep.
  -   Added new `hamster_swish` and `hamster_thump` sound effects that play during these transitions, making interactions feel more realistic and fun.
- **New Dispenser Behavior for Hamster Bedding**
  -   Dispensers can now be loaded with Hamster Bedding. When activated, they will shoot out a decorative puff of the new floaty leaf particles, consuming one Hamster Bedding item.
- **New Advancements for Wander Mode and Crafting**
  -   Added a new branch to the "The Hamster Life" advancement tab to guide players through the new features.
  -   New advancements include: "Artisanal Floor Mulch" (crafting bedding), "Luxury Leaf Pile" (crafting a bed), "Home is Where the Leaves Are" (linking a bed), "Sweet Dreams 'r Made of Leaves" (hamster sleeping in a bed), and a hidden challenge advancement for placing a bed upside-down.
- **New Jade Integration for Hamster Bed**
  -   If the Jade mod is installed, looking at a Hamster Bed will now display a detailed tooltip.
  -   It shows which hamster the bed is linked to, whether Wander Mode is active, the current wander distance, and whether the hamster is allowed to sleep in the bed. Sneaking reveals detailed instructions on how to control the bed's functions.
- **Added Compatibility for Croptopia, Immersive Engineering, Serene Seasons, and Thermal Expansion**
  - A huge thank you to [@CasualAnimalEnjoyer](https://github.com/CasualAnimalEnjoyer) for creating 30 new resource files to ensure out-of-the-box compatibility.
  - **Croptopia:** Food recipes now accept AHP vegetables via tags. Seed crafting recipes have been separated to ensure crafting an AHP vegetable yields an AHP seed, preventing conflicts with Croptopia's own seed recipes.
  - **Serene Seasons:** Cucumber and Green Bean crops now have defined growing seasons and will bloom accordingly.
  - **Tech Mods:** Added support for growing Cucumber and Green Bean crops in the **Immersive Engineering** Garden Cloche and the **Thermal Expansion** Phytogenic Insolator.
- **New Mod Icon**
  -   Huge thanks to [@The Retro Stitcher](https://theretrostitcher.com/) for re-designing the mod's icon!

### Changed
- **Announcement icon position in Creative Inventory**
  -   The announcement bell icon that appears in the Creative Inventory screen has been moved from the bottom-right corner to the top-right corner of the screen.
  -   This relocation prevents the icon from being overlapped by popular inventory utility mods such as JEI, REI, and EMI.
  -   The maximum configurable X/Y offsets for the creative inventory widget have also been increased from 100 to 500 to allow for greater user customization.
- **Improved the "Settle to Sleep" Animations**
  - Tweaked the "Settle to Sleep" Animations to make them considerably cuter and more compatible with scenarios where the hamster might be "jumping into bed."
- **Smarter Hamster Pathfinding (Bed Avoidance)**
  -   Hamsters now have a custom navigation system that makes them aware of Hamster Beds.
  -   By default, a hamster will now attempt to pathfind *around* any Hamster Bed that is not its own linked bed. After all, hamsters can be a bit territorial irl.
  -   To prevent hamsters from getting stuck in complex environments, they will only try a few alternate routes before giving up and taking a more direct path. This avoidance behavior can be disabled in the config.

### Fixed
- **Personality ID assignment for command-spawned hamsters**
  -   Resolved an issue where hamsters created using the `/summon` command were not being assigned a random "personality ID," causing them all to use the same animations and sitting poses.
  -   This was caused by the `/summon` command bypassing the `initialize()` method where the ID was being set.
- **Orange hamster spawning in Stony Shore biomes**
  -   Fixed a bug where orange hamsters were incorrectly spawning in the `minecraft:stony_shore` biome instead of the intended light/dark gray variants.
  -   The biome was being correctly excluded from the "white" variant spawn list but did not match any other specific criteria, causing it to fall back to the default orange variant. The spawn conditions for gray hamsters now explicitly include this biome.
- **Startup crash (`StackOverflowError`) related to the announcement system**
  -   Fixed a critical `StackOverflowError` crash that would occur on game launch if the "Snooze (Session)" feature had been used in the previous session.
  -   The crash was caused by a recursive loop in the `AnnouncementManager`'s initialization logic. The redundant check causing the loop has been removed.
- **"Snooze (Session)" functionality and state synchronization with Patchouli**
  -   The "Snooze (Session)" button now correctly snoozes only the specific announcement being viewed, rather than incorrectly disabling all notifications globally for the session.
  -   The underlying `disabled_until_launch` system, which was the source of the startup crash, has been completely removed and replaced with a non-persistent, in-memory list for session-snoozed items.
  -   Fixed a bug where announcements snoozed for the session would remain marked as "read" in the Patchouli guide book after restarting the game. The system now correctly syncs the "unread" status of all pending notifications with Patchouli's data when a world is loaded.
- **Ghost Shoulder Hamsters on Player Death**
  - Resolved a critical bug where shoulder-mounted hamsters would remain on the player after death, becoming permanently stuck. They will now correctly spawn at the player's death location in a 'knocked out' state, and a new config option allows players to keep them on their shoulder upon respawn if desired.
  - Implemented a backward-compatibility fix to ensure any hamsters that were previously stuck on a player's shoulder can now be dismounted correctly.
- **Stale triggerable animations playing when entity is rendered**
  -   Fixed a visual desynchronization where a one-shot animation (like the "Settle to Sleep" transition) triggered while a hamster was off-screen would play belatedly as soon as the hamster was rendered, causing it to be out of sync with the new sound effects.
  -   A server-side cancellation scheduler now ensures that any triggered animation automatically expires if it has not been played by a client within its expected duration.
- **Hamster and wild bush spawning on Sculk, Clay, and Moss blocks**
  -   Black hamsters can now correctly spawn on Sculk blocks, decreasing their unintentional rareness in the Deep Dark biome.
  -   Wild Green Bean and Wild Cucumber bushes can now generate on Clay and Moss blocks, allowing them to spawn more frequently in biomes like Swamps, especially if you have a mod that modifies the block palette of the ground in those areas.
- **Resolved "Unknown message ID" errors on dedicated servers (1.20.1)**
  -   Split the 1.20.1 network packet registration into distinct client-to-server and server-to-client methods. (This had already been done on 1.21.1)
  -   Ensured that the server correctly registers handlers for client packets (like render state updates), preventing disconnects or log errors when players interact with hamsters.
- **Corrected Gray Hamster spawning in deserts.**
  -   Gray hamsters were incorrectly spawning in sandy biomes like deserts, where Cream hamsters are intended to appear.
  -   Added the `c:is_sandy` tag to the Gray variant's exclusion list to prevent this overlap.
  -   **Note:** If you're still seeing Gray hamsters in deserts, navigate to `Spawn Settings > Variant Spawning > Gray Variants > Excluded Tags` in the config menu, right-click the setting, and select "Restore Defaults."
- **Fixed Uninteractable Hamsters with Force-Mount Keybind**
  -   Resolved a critical bug where enabling the "Force Shoulder Mount" keybind in the config would prevent all interactions with tamed hamsters.
  -   This was caused by the server trying to access client-side keybinding code.
  -   The logic has been refactored to use a network packet, ensuring interactions work correctly regardless of the keybind setting.
- **Resolved World Gen Config Weirdness**
  -   Previously, world-gen settings like spawn weights and biome lists were locked on the title screen because they were bundled with other synced server settings, making it impossible to configure them before creating a Singleplayer world.
  -   The configuration system has been restructured: clicking "Adorable Hamster Pets" in the mod menu now opens a new landing page with two distinct buttons: **"Main Settings"** (gameplay & client options) and **"World Gen Settings"**.
  -   The new "World Gen Settings" menu uses a special save mode (`SaveType.SEPARATE`), allowing you to freely edit your local defaults for Singleplayer worlds while still respecting server-side overrides when joining a Multiplayer server.

---

## [3.3.2] - 2025-10-07

# The More Polite Notifications Patch

## The announcement system was always intended as a personal touch—a cozy newspaper to share news and updates directly while you sip your coffee. But your feedback made it clear that 'personal touch' can feel a bit intrusive when you've got two or three hundred other mods vying for your attention. I get it.

This patch is all about handing the controls over to you. Based on some fantastic feedback, I've added a suite of new more accessible options to let you decide exactly how—or if—you see notifications. The goal is to keep the feature useful for those who want it, while making it completely unobtrusive for those who don't.

### Added
- **Added click-and-drag functionality to the announcement screen scrollbar.**
  -   You can now scroll down on announcements by dragging the scrollbar handle with your mouse, in addition to using the mouse wheel.
- **Enhanced control over the announcement system with new UI options and quality-of-life features.**
  -   A new "Enable Announcements" master switch has been added to the config screen, allowing you to control all notification icons at once.
  -   You can now instantly mark all announcements as read from a new button in the config screen.
  -   A "Snooze (Session)" button is now available on the announcement screen to temporarily hide all notifications until the game is restarted.
  -   Holding `Shift` while clicking "Mark As Read" on any announcement will now mark all pending notifications as read.

### Changed
- **The inventory key now closes the announcement screen.**
  -   Pressing the inventory key (default 'E') while viewing an announcement will now correctly close the screen, mimicking standard Minecraft UI shenanigans.
- **Overhauled the announcement screen's action buttons for clarity and control.**
  -   The button layout has been reorganized into two rows and simplified. The old, confusing "Disable These" button has been replaced with a clearer "Disable All" button that permanently turns off the notification icons (this can be undone in the config).
- **Patchouli's "Mark All as Read" button now includes announcements.**
  -   The "Mark All as Read" button on the main page of the Hamster Tips guide book now correctly marks all pending announcements as read in both Patchouli's system and mine, so now it behaves exactly as you'd expect it to.

### Fixed
- **Resolved major UI scaling issues with the announcement screen.**
  -   The entire announcement GUI (background, content, and buttons) now dynamically scales down to fit the available window space, preventing elements from being cut off.
- **Attempted fix for a startup crash on NeoForge with large modpacks.**
  -   A `NullPointerException` was reported by one person on game launch, likely caused by a race condition with the new announcement system in heavily modded environments. While I couldn't reproduce the crash myself, I've refactored the system to initialize itself on demand, which should resolve this kind of loading order issue. This change is safe and may prevent similar problems in the future.
- **Removed the concept of "mandatory" announcements.**
  -   You are now the master of your own notification destiny. The system no longer distinguishes between optional messages and "mandatory" update notifications, giving you full control to dismiss, snooze, or disable any and all announcements as you see fit.
- **Upgraded Fzzy Config library to resolve UI bugs.**
  -   Updated from v0.7.0 to v0.7.3 to fix an issue where nested, collapsed groups in the config screen would incorrectly appear expanded by default. This should make navigating the configuration screen much less overwhelming.
  -   This Fzzy Config update also resolves a data generation bug that was preventing tooltips from appearing for the "Survival Inventory" and "Creative Inventory" announcement bell icon offset settings.

---

## [3.3.1] - 2025-10-06

# The Server Sanity Patch

## This is a quick but critical hotfix to address a series of unfortunate events that followed the 3.3.0 update, primarily aimed at making dedicated servers not immediately burst into flames.

Long story short: dedicated servers should now launch without issue, and the new announcement system will no longer crash your client for the crime of being too efficient. Your game should be much happier now.

### Fixed
- **Resolved multiple dedicated server crashes on startup.**
  -   Fixed an `AbstractMethodError` crash by correctly separating the registration of client-side packet handlers from the common registration of packet types.
  -   Fixed an `InvalidMixinException` crash by moving several client-only mixin accessors (`ClickableWidgetAccessor`, `GuiBookAccessor`, etc.) to the client-specific section of the mixin configuration file.
- **Fixed a `NullPointerException` when dismissing a single announcement.**
  -   This occurred when the notification icon was clicked from an inventory screen with only one pending notification. The system now correctly uses the fully initialized virtual `BookEntry` from Patchouli's data, preventing the crash.
- **Corrected the "See Changelog" button URL in the announcement screen.**
  -   The URL was missing the required version and loader suffix, causing it to lead to a 404 page on Modrinth. It now generates the correct link for the specific version.

---

## [3.3.0] - 2025-09-14

# **The Patchouli Page-Turner Update**

## Hamster Tips guide book graduates to Patchouli, and your game now gets a special update notes system that can pull live from my GitHub repo.

THe Hamster Tips guidebook is completely overhauled with Patchouli's built-in tools + my own custom UI, smarter text wrapping, and automatic upgrades from the old guide book. A physics-simulated notification bell (with hamster ears) flags unread notes— all configurable and snoozeable— and virtual entries in Hamster Tips open the new markdown viewer without leaving the game. Under the hood, a new smart ownership algorithm ends pet-on-pet friendly fire, and translators also get a tidier language file.

### Added
- **New Patchouli Guide Book**
  -   Replaced the original vanilla written book with a comprehensive, feature-rich guide book powered by the Patchouli library.
  -   Features a completely custom user interface, including a unique book texture and a custom-coded landing page layout with a wrapped, multi-line subtitle for a more polished presentation than what Patchouli offers by default.
  -   Implemented a dynamic text wrapping and pagination system for entry titles, ensuring that long or translated titles do not render off the edge of the page.
  -   Includes a robust backwards-compatibility system that automatically detects and upgrades any old guide books found in a player's inventory or any opened container to the new Patchouli version.
- **Client-Side Announcement & Update Notification System**
  -   Implemented a robust client-side notification system capable of fetching a `manifest.json` and individual markdown files from a public GitHub repository.
  -   This allows for the delivery of announcements and "What's New" update notes to players without requiring a mod update.
  -   The system intelligently compares the player's installed mod version against the latest version in the manifest to create "Update Available" notifications, which can be snoozed.
- **Dynamic HUD & GUI Notification Icon**
  -   Added a new notification icon (a bell with hamster ears) that appears on the main game HUD and compatible GUI screens when unread notifications are pending.
  -   The icon is driven by a custom, render-frame-timed physics animator using a spring-damper model to create organic, natural motion. When moving between positions (e.g., when opening the recipe book), the icon smoothly accelerates and decelerates, with a slight rotational kick and overshoot for a more dynamic feel.
  -   Upon reaching its destination, it performs a brief "settle wobble" before coming to a rest. The icon also features a periodic idle wiggle animation, and smoothly scales up on hover for satisfying tactile feedback.
  -   The icon dynamically repositions itself to remain anchored to the corner of inventory screens, organically transitioning to its new location when the GUI shifts, such as when opening or closing the recipe book.
  -   Its tooltip is dynamic, displaying context-aware text like "New update available," "New announcement," or "What's new in vX.Y.Z."
- **Custom Markdown-Powered Announcement Viewer**
  -   Created a new, custom GUI screen for displaying announcement and update content, replacing the standard Patchouli entry page for these notifications.
  -   The viewer features a full Markdown renderer that supports headings, bold/italic text, lists, code spans, clickable links, and dividers.
  -   Includes a set of vanilla-style buttons for actions like "Mark as Read," "Remind Me Later" (snooze), "Disable These," and "See Changelog," with dynamic positioning depending on which buttons are present.
- **Virtual Patchouli Integration for Notifications**
  -   Virtually injects two new categories ("Announcements" and "Update Notes") and their corresponding entries into the Hamster Tips guide book at runtime.
  -   This leverages Patchouli's list-rendering and unread-marker system without requiring any physical json files in the mod's JAR.
  -   Clicking a virtual entry opens the custom announcement GUI instead of the entry (because there is no entry), and mixins have been added to prevent the "Mark All Read" button from affecting these virtual entries, and to prevent them from showing up in main "Entry Index" list.
- **New Announcement Icon Configuration Options**
  -   Added extensive new settings to the config to control the announcement system, including toggles for the HUD and in-GUI icons.
  -   Players can customize the HUD icon's screen position, scale, and offset, as well as the offset for the in-GUI widget icon.
  -   New `ConfigAction` buttons allow players to reset their entire announcement history or re-enable optional announcements if they were previously disabled.
- **New Configuration Options for Health and Mounting**
  -   Added `Wild Hamster Max Health` and `Tamed Hamster Max Health` settings, allowing players to customize the health pools for both wild and tamed hamsters independently.
  -   Added an `Enable Force-Mount Keybind` option (disabled by default) that, when enabled, allows players to mount a nearby tamed hamster by pressing a dedicated keybind, without needing to hold a specific item.
  -   Added a `Consume Shoulder-Mount Item` toggle (enabled by default) that controls whether the item used to lure a hamster onto the player's shoulder is consumed in the process.
- **Farmer’s Delight Cutting Board Compatibility**
  -   Added cutting board recipe support for cucumbers → sliced cucumbers.
  -   Thanks to [Cashhew](https://discord.com/channels/1382334723333820568/1382334724000841847/1420075931992850442) for the contribution!
- **Smart Pet-Ownership Algorithm (under the hood)**
  -   New logic that recognizes your pets across Minecraft’s many creatures (wolves, cats, parrots, horses—and most modded pals).
  -   It doesn’t just “check a box” — it can figure out ownership even when a pet only stores a hidden ID instead of a visible owner link.
  -   Built to prevent friendly-fire even with all the chaos: thrown hamsters, indirect hits, and weird edge-cases from other mods. (See "Fixed" section for more details)
  -   Designed to be fast and lightweight so it won’t slow your world down.
  -   Future-proofed: works across loaders and should play nicely with most other mods out of the box.

### Changed
- **Optimized Language File Generation**
  -   Refactored the data generation process for the `en_us.json` language file to eliminate the creation of redundant, prefixed translation keys for the configuration screen.
  -   This results in a smaller, cleaner, and more efficient language file with no impact on in-game text. Easier translating!

### Fixed
- **No More Pet-on-Pet Drama**
  -   Thrown hamsters now bounce off your own tamed pets (wolves, cats, parrots, horses—and most modded pets) instead of hurting them.
  -   I designed a friendly-fire **smart ownership algorithm** that figures out who owns what—even when a pet only remembers you by an internal ID.
  -   Works both ways: your other pets can’t hurt your tamed hamsters either.
- **Resolved Server Crash on Hamster Throw**
  -   Fixed a `NullPointerException` that would crash dedicated servers when a player used the "Throw Hamster" keybind.
  -   The crash was caused by server-to-client sound packets being registered only on the client, leading to an error when the server attempted to send them.
  -   All network packet registrations have been consolidated into a single, common method, ensuring both the server and client are aware of all packet types.
- **Corrected Configuration Synchronization**
  -   Replaced the incorrect `@ClientModifiable` annotation with the appropriate `@NonSync` annotation for all client-side settings in the configuration file.
  -   This change prevents a potential issue where clients could modify server-synced settings without permission and ensures that client-only settings (like UI and animation options) are handled correctly.

---

## [3.2.0] - 2025-08-23

# **The Multi-Shoulder Hamster & Mega-Configurability Update**

### Just when you thought your shoulders were safe, I've gone and turned them into a hamster condominium. This update completely overhauls the shoulder pet system, adds a frankly irresponsible amount of configuration, injects a hefty dose of unnecessary realism, and more!

### Added
- **Multi-Hamster Shoulder & Head Mount System**
  -   Players can now have up to three hamsters mounted simultaneously: one on each shoulder and one on their head.
  -   The mounting system will automatically place a hamster in the next available slot (Right Shoulder -> Left Shoulder -> Head).
  -   Shoulder-mounted hamsters will automatically adjust their offset to fit both slim and wide player models, and will sit even further out to rest on top of equipped chestplates.
  -   A configurable dismount order (`LIFO`/`FIFO`) determines which hamster is removed first by the standard dismount key.
  -   Added a client-side hand-swing animation when mounting a hamster to the player's shoulder.
- **Dynamic & Reactive Shoulder Hamster Animations**
  -   Shoulder-mounted hamsters are now fully animated using their primary GeckoLib model and no longer use a separate, static vanilla model.
  -   They dynamically cycle through standing, sitting, and unique, slot-specific "laying down" animations.
  -   Hamsters now react when the player sprints by entering their "laying down" animation, as if holding on for dear life. A randomized micro-delay ensures each hamster reacts independently.
- **Physics-Based Animation for Shoulder Hamsters**
  -   Shoulder-mounted hamsters now have a dynamic, physics-lite simulation applied to them, making them feel more alive.
  -   **Vertical Bounce:** Hamsters will bounce on the player's shoulder in response to jumping and falling, with their motion driven by the player's vertical velocity.
  -   **Squash and Stretch:** The hamster's model will visually squash and stretch to simulate inertia and impact forces, making jumps and landings feel more impactful.
  -   **Impact Effects:** A unique squash effect and a new custom sound effect will now trigger at the moment a hamster lands back on the shoulder after a fall or jump, with the sounds staggered for a more natural feel when multiple hamsters are mounted.
- **Configuration Options for Shoulder Hamster Audio**
  - Added a "Silence Idle Sounds" toggle to mute the ambient squeaks from shoulder hamsters.
  - Added a "Silence Physics Sounds in First-Person" toggle to mute the new landing/bounce sound effect specifically when in first-person view.
- **Configurable Animation Control for Shoulder Hamsters**
  -   Added a config group for "Animation Settings" to give players full control over shoulder hamster behavior.
  -   Players can disable the "Dynamic Animations" toggle to force all shoulder hamsters into a single, static pose using the "Forced Animation State" dropdown.
  -   The duration of each dynamic state can be customized/randomized with "Min/Max Animation State Duration" sliders, controlling how long a hamster will stay in one pose before transitioning.
  -   Added a `forceLayDownOnWalk` config option (disabled by default) that makes hamsters enter their "laying down" pose during any player movement, not just sprinting.
- **Config-Driven Item Tags for All Interactions**
  -   All hardcoded item checks have been replaced with a high-performance, config-driven system, allowing users to customize nearly every hamster interaction.
  -   Players can now define custom lists of items or item tags for taming, standard feeding, buff foods, shoulder mounting, cheek pouch unlocking, and basically every other hamster interaction.
  -   This new system includes built-in default compatibility for popular mods like **Farmer's Delight** and its addon **Cultural Delights**.
- **Conditional Headshake Animations**
  -   Added a new `anim_hamster_moving_headshake` animation.
  -   When a hamster refuses a food item, it will now intelligently play either a stationary or a moving headshake animation depending on whether it is standing still or walking.
- **Visual Height Adjustment on Snow Layers**
  -   Hamsters will now render slightly higher when standing on snow layers, preventing their model from sinking into the snow and creating a more realistic and visually polished effect. Now your white hamsters won't be so invisible in snow biomes! Lol.
- **Fresh Moves Compatibility for Shoulder Hamsters**
  -   The new shoulder hamster rendering system automatically supports player animation-overhaul resource packs like [**Fresh Moves**](https://modrinth.com/resourcepack/tras-fresh-player), because shoulder hamsters are now anchored directly to the player's animated body parts (arms and head), allowing them to realistically move and bounce along with the player's enhanced walk cycle.

### Changed
- **AppleSkin Compatibility for Configurable Food**
  -   The `CheeseItem`'s properties are now fully dynamic*, ensuring that changes to its nutrition or saturation values in the config are correctly reflected in AppleSkin's "on-eat" HUD preview. **Unfortunately due to major API changes between MC versions, this only works on 1.21.1.*
- **Shoulder Mounting Behavior**
  -   The logic has been updated to allow players to mount a hamster onto their shoulder with a lure item (like cheese) even while sneaking.
- **Wander AI Frequency**
  -   The default chance for a hamster's idle `WanderAroundFarGoal` to activate has been slightly increased, making them a bit more active when not following the player.
- **Reworked mob fleeing behavior from hamsters.**
  - Replaced the hardcoded Creeper-fleeing mechanic with a new configurable system. Ravagers and Spiders will now flee from hamsters by default, restoring the unique anti-Creeper role to vanilla cats. These new fleeing behaviors can be individually toggled in the config.
- **Hamster Textures**
  - Increased the saturation of the lavender hamster variant's texture to make it appear more purple and less gray.
  - Slightly tweaked two of the white overlay textures to make them look a little bit cuter— very subtle changes here.

### Fixed
- **Hamster Teleportation**
  -   Fixed a bug on 1.20.1 where hamsters would not teleport to their owner.
- **Baby Shoulder Hamster Rendering Offset**
  -   Corrected the vertical offset for baby shoulder-mounted hamsters, ensuring they now move up and down correctly with the player's model when sneaking.
- **Hamster Suffocation**
  -   Implemented a "self-rescue" mechanic for hamsters that have been thrown. If they somehow manage to phase inside a block, they will now automatically teleport upwards to the nearest safe block, preventing suffocation deaths. 
- **AI Goal Visual Flickering**
  -   Resolved a visual glitch where the hamster's head would flicker during its wander animation by preventing the `LookAt` and `Wander` goals from running at the same time.
  -   The effective range of the `HamsterLookAtEntityGoal` was slightly reduced to prevent the hamster from looking up at an unnatural angle when the player is nearby but not directly adjacent.

---

## [3.1.2] - 2025-08-13

## <font color="GOLD">If you’re thinking _hey, this changelog looks awfully familiar…_ you’re not wrong.</font>

### This is essentially the same feature set as 3.1.0 — but with one *critical* difference: these jars are ACTUALLY BUILT CORRECTLY. 

Version 3.1.0’s jars (especially for 1.20.1) were missing a bunch of generated data files, because I had just finished implementing a new CI (Continuous Integration; i.e. 'automated workflow that publishes my jar files for me') configuration, and it had a slight but not so slight oversight. I’ve removed that release from Modrinth and CurseForge so nobody new grabs the broken jars. This is the fixed re-release, under a new version number, so everyone’s launcher will properly prompt for an update.

### Added
- **Sunflower De-Modding Station**
  -   Added a simple shapeless crafting recipe to convert one custom, harvestable sunflower back into one vanilla sunflower. For when you need the original for... reasons.
- **Advanced Dismount Engineering**
  -   To prevent accidental hamster ejections when dismounting a horse or building a bridge in the nether (don't ask), the shoulder dismount mechanic is now highly configurable.
  -   Players can now choose between using the `SNEAK` key or a new, separate `CUSTOM_KEYBIND` for dismounting.
  -   Additionally, the action can be set to trigger on a `SINGLE_PRESS` or a quick `DOUBLE-TAP`, preventing conflicts with toggle-sneak or bridge-building. For the double-tap setting, the delay is also configurable. Find the new settings in the "Shoulder Hamster Settings" config group.

### Changed
- **Internal Hamster Wiring Overhaul**
  -   The hamster's entire internal state-tracking system has been refactored. I've packed seventeen separate on/off switches (booleans) into one glorious, hyper-efficient integer using bit-masking. This change is purely under-the-hood but drastically reduces the chances of mod conflicts and crashes related to entity data. This was specifically to address a mod conflict on 1.20.1 with the Sortilege mod. Apparently it's trying to track a lot of data on Entities, and my mod is also doing that, so they had a conflict on slot #41. But even just in general, your hamsters are now more stable and less likely to have an existential crisis when another mod is present.

### Fixed
- **Publishing Mishap Autopsy**
  - In 3.1.0, my CI pipeline built jars without running the data generators, so critical JSON assets never made it into the finished files. This slipped by on 1.21.1 because those generated files happened to be present from earlier work, but 1.20.1 exposed the issue immediately with crashes. However, I did not notice these crashes myself because I test with manually built jar files, not the ones that the CI pipeline automatically creates. This has now been fixed at the Gradle level so the problem cannot recur — `runDatagen` is now a build dependency for all relevant jar tasks.
- **Corrected Overly-Trusting Hamsters**
  -   Hamsters will no longer beg for food from any random player waving a cucumber slice. They will now only perform their adorable begging routine for their rightful owner, as intended. And for that matter, I also fixed a similar issue with the Diamond Stealing, Knocked Out, Diamond Sniff Celebration/Sulking, & Pink Petal Application/Cycling/Removal interactions, all of which were in the wrong spot in my interactMob method, which meant they were sneaking past the "is player the owner?" check.
- **NeoForge Composting and Key-bind Registration**
  -   Fixed an issue where compostable items (seeds, crops) were not functional on the NeoForge version of the mod. They now correctly register as compostable.
  -   The "Throw Hamster" keybind, previously missing on NeoForge, now correctly appears in the Controls menu.
- **Resolved Critical Mod Incompatibility Crash**
  -   Fixed a hard crash caused by a `DataTracker` ID collision when running alongside certain other mods (like Sortilege on 1.20.1). The internal wiring overhaul (see "Changed") resolves this issue by significantly reducing the number of data slots the hamster entity occupies.

### B.T.S.
Here's how the superbly juicy magnificent and fantastic `publish.yml` file works:
* Automatically builds both Fabric/Quilt and NeoForge versions in a single workflow
* Publishes each build to both Modrinth and CurseForge with correct, consistent file names
* Attaches a specific snippet of my changelog content directly from the separate public repository for each release
* Includes explicit dependency listings for both Modrinth and CurseForge to ensure proper mod loading
* Supports multiple Minecraft versions with separate version strings for each loader
* Allows manual or tag-based triggering for flexible release management
* Keeps build and publish logic in one place for easier maintenance and fewer manual steps

Anyway IT'S AWESOME

---

## [3.0.1] - 2025-08-02

### Fixed
- **Spammy Console Icky Gross Stuff**
  -   Fixed my spammy `.info` level logging in `HamsterFollowOwnerGoal.java` that I had forgotten to switch back to `.trace`.

---

## [3.0.0] - 2025-07-27

# **The Heist & The Haste Update**

## Your hamsters have developed expensive taste, learned new languages, and also contracted the zoomies.

Introducing the brand new **Diamond Stealing** mechanic, where your furry companions will snatch your valuables and challenge you to a game of keep-away. Feed them some **Steamed Green Beans** and witness the true meaning of "zoomies" as they tear around you in a chaotic, super-charged sprint. 

This update also includes a complete, config-driven overhaul of the entire biome spawning system for incredible mod compatibility, smarter AI, new animations, and a ton of critical bug fixes.

#### <font color="red"> **IMPORTANT:** Due to the extensive changes to the spawning system, it is **highly recommended** that you delete your existing config file (`/config/adorablehamsterpets/main.toml`) before loading a world with this version. This will allow the new, more detailed settings to generate correctly. Your old settings will be lost.</font>

---

### Added
- **Russian, Chinese, and Korean Language Support**
  -   Ever heard a hamster squeak in Chinese? I didn't think so. But now, you can! Or perhaps you would prefer a little bit of cooing in Russian... or a squeal in Korean? Thanks to a few of the awesome people in The Cheek Pouch Discord channel, the mod has been fully translated to three languages and more on the way!
- **New Major Feature: Item Stealing & Chase Mechanic**
  -   Tamed hamsters now have a configurable chance to notice valuable items dropped on the ground (defaults to just diamonds).
  -   Nearly every aspect of this feature is configurable, including which items can be stolen (you can add any item, multiple items simultaneously, and even items from other mods!).
  -   When they spot one, they'll perform a unique pounce animation and snatch the item, initiating a playful game of keep-away. The pounce sound effect is even dynamic, changing based on the material of the stolen item in case you modify it in the config!
  -   While in "heist mode," the hamster will flee if you get too close and cheekily taunt you from a distance with a new animation, encouraging a chase.
  -   The stolen item is visually rendered in the hamster's mouth, synced with all head movements and animations.
  -   To get your item back, you have to catch your speedy thief and right-click it. If you take too long, the hamster will get bored and drop the item. But then, hamsters have a short memory, so it will probably pick it up again.
- **New Major Feature: "Zoomies" for Buffed Hamsters**
  -   Hamsters under the effect of Steamed Green Beans will now get the "zoomies," a burst of energetic, high-speed activity.
  -   Instead of wandering aimlessly, they will sprint in dynamic, chaotic circles around their owner.
  -   When following their owner, they will no longer run in a straight line but will instead dart around randomly in the owner's general direction.
  -   This frenetic behavior is accompanied by a new custom particle trail that bursts outwards from the hamster as it sprints.
- **New Sprinting Animation & Standardized Speeds**
  -   A third-tier "sprinting" animation has been added for the hamster's highest speeds, used during zoomies and fleeing.
  -   All hamster movement speeds have been standardized across all AI goals for more consistent and predictable behavior.
  -   Walking and running animations are cuter.
- **Enhanced Idle Animations**
  -   Hamsters will now occasionally play one of three new, randomized "looking up" animations when their `LookAtEntity` AI goal is active and they are looking at a nearby target (like you).
- **New Wild Hamster Settle to Sleep Animations**
  -   Hamsters will now play one of three new, randomized "settle to sleep" animations, paired with the three already-existing sleeping animations when going to sleep, similar to how tamed hamsters already did when sitting.
- **Configurable Biome Spawning System (Major Rework)**
  -   The entire spawning system for both hamsters and world-gen features (bushes, sunflowers) is now controlled by the config file.
  -   Users can now define exactly where things spawn using biome IDs, vanilla biome tags, and "convention tags" (`c:is_cold`, `c:is_forest`, etc.) for vastly improved compatibility with modded biomes from packs like Terralith and Biomes O' Plenty.
  -   Each feature and the hamster itself has its own set of include/exclude lists and tag lists, offering granular control over world generation.
- **New Faster Throw Velocity for Buffed Hamsters**
  -   Throwing a hamster while it's under the effect of the Steamed Green Bean buff will now launch it at nearly double the default velocity.
  -   Both the default and buffed throw velocities are now configurable in a new "Hamster Yeet Settings" group in the config.
  -   (A special thanks to `@Petite` on Discord, who planned to defeat the Ender Dragon using only hamster projectiles. This idea inspired me to ensure they had enough velocity to reach the dragon in the first place! 😂)
- **Improved Shoulder Dismount Logic**
  -   When the player dismounts the hamster from their shoulder, it will now be placed at the location where the player was looking if it was safe and within reach. If not within reach, it will spawn at the player's feet.
- **New Config Option**
  -   Added a config option to change the default name of the hamster entity to "Hampter" instead of "Hamster," because I wanted to, so I did.

### Changed
- **Spawning Logic Overhaul**
  -   The internal logic for determining hamster color variants has been completely rewritten to be "hamster-centric." It now uses a combination of specific biome checks, vanilla tags, and universal convention tags to ensure hamsters spawn in thematically appropriate locations, even in heavily modded worlds. Note: I only had time to test this with Terrralith— so if you're using a different biome generation mod (Biomes O' Plenty, Oh The Biomes You'll Go) you may see more frequent orange hamsters, since they are the default fall back. Or maybe it will work perfectly! Lol.
  -   The custom sunflower replacement system is now also tag-aware. It will now correctly replace vanilla sunflowers with the harvestable version in any biome that is supposed to have them (including modded biomes), not just the vanilla Sunflower Plains.
- **Diamond Seeking AI Improved**
  -   The independent ore-seeking AI is now smarter. Hamsters will now prioritize pathfinding to exposed ores that are visible to the player before considering ores that are completely buried.
  -   A new particle breadcrumb trail has been added. When a hamster is leading you to an ore, it will now create a faint trail of dust particles along its intended path, making it much easier to see where your tiny prospector is headed.
- **Death Message Attribution**
  -   Death messages will now credit the hamster by its name (e.g., "Player was slain by Hampter") instead of attributing the kill to the hamster's owner. Hamsters are now fully complicit in their actions. 😁
- **Dependencies**
  -   The Jade mod is no longer a required dependency and is now correctly listed as optional for all mod loaders. The game will no longer crash if Jade is not installed.
- **Pathfinding & AI**
  -   Hamster pathfinding penalties around hazards have been strengthened. They will now be much more reluctant to pathfind into or near fire and lava.
  -   The idle AI goals (Wander, Look At, Look Around) have been re-balanced to prevent them from fighting with each other, resulting in much more natural and less "flickery" idle behavior.
- **Textures & Animations**
  -   The textures for the Blue and Lavender hamster variants have been updated with slightly increased saturation to make their colors more distinct and recognizable without being neon.
  -   The textures for the Wild Cucumber Bushes have been slightly desaturated to help them blend more naturally into the warmer, drier biomes where they typically spawn.
  -   The transition speed between animations has been reduced from 5 ticks to 2 ticks, resulting less of the "hamster sliding across the ground" type of effect when it is transitioning between movement animations.
  -   The rotation speed of the hamster has been overhauled to account for it not having a separate head and body like vanilla models, resulting much more snappy rotations and accurate target-facing. Vanilla Minecraft mobs move their head first, and body follows second at a much slower pace. Since my hamsters use Geckolib for the model, they do not have a separate head and body in the same way that vanilla mobs do, which resulted in a quick initial turn where the hamster would turn halfway toward its target, and then a slower finish. I basically combined both of them together to eliminate that weird behavior.
  -   The speed of the running animation has been decreased by 25% to better differentiate it from the new sprinting animation, giving it a more natural pace.

### Fixed
- **Fixed Shoulder Hamsters Being Deleted on Player Death**
  -   Resolved a critical bug where a shoulder-mounted hamster would be permanently deleted if the player died, even with `keepInventory` enabled. The system now correctly detects the player's respawn and safely dismounts the hamster at the player's death location.
- **Fixed Hamster Suffocation on Dismount/Throw**
  -   Implemented a robust safe-spawning algorithm that performs a multi-stage search for a valid, non-obstructed location when a hamster dismounts or lands from a throw. This should completely resolve all known suffocation bugs.
- **Fixed Right-Click Stack Splitting in Hamster Inventory**
  -   Corrected the underlying inventory logic to properly handle the `amount` parameter when removing items. Players can now correctly right-click to split stacks of items inside the hamster's cheek pouch inventory, just like in a vanilla chest.
- **Fixed Thrown Hamster Particle Desync**
  -   Adjusted the spawn position calculation for the in-flight particle trail. The trail now correctly originates from the hamster's model instead of appearing slightly ahead of it, which was most noticeable when viewing from a second player's perspective or with replay mods.
- **Fixed Black Hamster Overlay Spawning**
  -   Wild black hamsters will now only spawn with their solid base color. This makes their appearance in dark environments like caves more thematic and reserves their white overlay patterns as a special trait achievable only through breeding.

---

## [2.3.0] - 2025-07-16

# 1.20.1 Forge/Fabric/Quilt support has arrived!

## After a long and surprisingly complex battle with the Forge 1.20.1 datapack and build systems, the mod is now fully functional on Forge. Your hamsters are now free to cause chaos across multiple mod loaders.

### Added
- Official support for Minecraft 1.20.1 for Forge and Fabric.
- Official Quilt support for 1.21.1 and 1.20.1

### Changed

- Improved hamster dismount logic. Hamsters will now be placed at the block the player is looking at (within a 4.5 block range), providing more precise and intuitive placement. I don't know about you, but when I take a hamster off my shoulder in real life, I get to choose where I put it down. It just made sense lol
- **Particle Changes:**
  - The ominous trial spawner particles (diamond celebration) and GUST particles (hamster flight trail) both had to be switched out since they don't exist in 1.20.1. I'm using composting particles for the diamond celebration since they have an upward bias, and cloud particles for the hamster flight trail.

### Fixed
- Fixed a critical bug where hamsters could suffocate inside blocks when dismounted from the shoulder or after impacting a block from being thrown. A robust safe-spawning algorithm now works in tandem with the new dismount logic to ensure hamsters are always placed in a valid, non-obstructed location.
- Fixed a visual bug where the particle trail for a thrown hamster would appear ahead of its model during flight.
- Removed the non-functional Hamster Guide Book from the creative mode inventory tab to prevent confusion.
- Resolved a critical issue on Forge where no custom world generation (hamsters, wild bushes, custom sunflowers) would occur.
- Fixed a startup crash on Forge related to the Hamster Spawn Egg item registration order by implementing a platform-specific solution.
- Wrestled the Forge build process to properly include all necessary models, textures and data files.
---

## [2.0.2] - 2025-07-16

### Fixed
- Fixed an issue where Wild Bushes and custom Sunflowers were not generating in the world on the NeoForge version of the mod.
- **Developer's Note:** You might be thinking, "Didn't you *just* fix a spawning bug?" You're right! As it turns out, the way NeoForge handles adding *entities* to the world is similar to how it handles adding *features* like plants, but yours truly is just now learning this. Lol. Thanks for your patience as I navigate the wonderful quirks of multi-loader development. Incidentally, unlike with the hamster spawning fix, if any of you already spawned in a world on NeoForge, just FYI— this fix will only take place in newly generated chunks.

---

## [2.0.1] - 2025-07-06

### Added
- **Creeper Behavior:** Creepers will now flee from hamsters, similar to their behavior with cats. This should prevent them from detonating near your beloved pets and landscape. Thanks to [mikabean999](https://www.reddit.com/user/mikabean999/) on Reddit for the idea.
- **Cheese Configurability:** Added new configuration options to control the hunger and saturation values restored by eating Cheese, allowing server owners to better balance food.

### Fixed
- Resolved a critical issue where Hamsters would not spawn naturally in the world on the NeoForge version of the mod. This was caused by an incorrect implementation of the biome modification logic for the NeoForge loader. The fix replaces the faulty Java-based event listener with a data-driven biome modifier JSON that correctly hooks into NeoForge's world generation pipeline.
- Fixed a bug that caused the hamster's cleaning sound to loop indefinitely if the hamster was commanded to stand up mid-animation.
- Fixed a bug where wild hamsters would incorrectly perform the cleaning animation while sleeping. This behavior is now correctly restricted to tamed, sitting hamsters.

---

## [2.0.0] - 2025-07-02

# Neoforge support has arrived!

## **Added Architectury Multi-Loader Support:** The entire mod has been refactored from a Fabric-only project to an Architectury project, enabling official support for both Fabric and NeoForge from a common codebase.

### Also Added
- A new configuration option, "Gold 'Mistake' Chance", to control the probability of a hamster mistakenly finding gold instead of diamond.
- A startled jump, complete with a bounce sound effect, that plays when a hamster "mistakenly" finds gold ore.
- A new message that appears when a hamster finds gold, in case that feature might be confusing for new players who expected it to find diamond.
- Two additional sound variants for eating cheese to make the audio feel more natural.
- The currently playing animation name will now display on the Jade debug overlay for easier diagnostics.

### Changed
- **Complete Refactor to Architectury API:** The entire mod has been refactored from a Fabric-only project to a cross-platform Architectury project. This makes the codebase platform-agnostic and ensures future features can be developed for both loaders simultaneously. Key changes include:
  - Replaced all Fabric API registries (for Items, Blocks, Entities, etc.) with Architectury's deferred registration system.
  - Replaced the Fabric Networking API with the Architectury Networking API, centralizing all packet handling into a single `ModPackets` class.
  - Replaced Fabric-specific events (Player Join, Commands) with their cross-platform Architectury equivalents.
  - Replaced the Fabric-only `AttachmentType` system for shoulder riding with a vanilla `DataTracker` system, mimicking the vanilla parrot mechanic for better stability and compatibility.
  - Implemented an `@ExpectPlatform` bridge to handle loader-specific spawn restriction logic, keeping the common codebase clean.
  - Resolved numerous NeoForge-specific loading and registration crashes by implementing native event handlers for renderers, model layers, and screens, ensuring compatibility with the NeoForge lifecycle.
  - Replaced a Fabric-only access widener with a cross-platform Mixin Accessor to ensure custom AI goal logic works on both loaders.
  - Corrected the NeoForge build script to properly include data-generated assets and Mixin configurations, fixing missing textures, translations, and features.
- Adjusted spawn locations for Wild Cucumber Bushes and Wild Green Bean Bushes. Cucumber Bushes (for the essential taming item) are now much more common and appear in a wider variety of temperate biomes. Green Bean Bushes (for the optional buff item) are now less common and are focused in more specific "lush" or "wet" biomes.
- Lavender hamsters now spawn in Mushroom Fields as well as Cherry Groves, making them more discoverable. It just fits. They are almost the color of Mycelium after all.
- **Hamster Model:**
  - The base hamster model has been scaled down 20% for cuteness, and so it better matches the scale of the shoulder model.
  - The shoulder-mounted hamster model has been scaled down 10% (it was already smaller than the base model) so it fits better on the player's shoulder, and raised slightly so it doesn't clip into the shoulder as much, but not too much so it doesn't look like it's floating.
- The volume of the hamster cleaning sound has been doubled. It was actually so quiet it was not even audible with the Sound Physics mod installed.
- The volume of the cheese eating sound effect has been increased by 20%. Hehe.
- **Hamster Animations:**
  - Improved the running and walking animations while I was creating an entirely new “sprinting” animation which will be hopefully released in the next update.

### Fixed
- **Resolved a critical server crash** caused by client-side classes (`Screen`, `SoundInstance`, `MinecraftClient`) being referenced in common code. All client-only logic for the guidebook, sounds, and particles is now correctly handled on the client, allowing the mod to run on dedicated servers.
- Fixed multiple crashes caused by illegally casting to a Mixin class (`PlayerEntityMixin`) instead of its proper accessor interface (`PlayerEntityAccessor`) when interacting with or throwing a hamster.
- Fixed a bug where `HamsterTemptGoal` would incorrectly activate while a hamster was in the "sulking" state.
- Prevented hamsters from starting or continuing cleaning while knocked out. I didn't even realize this bug was there until I added the cleaning sound effect, and then I started hearing it while the hamster was knocked out. Lol.

---

## [1.2.1] - 2025-06-12

### Fixed
- Resolved a critical crash that would occur when exiting a world to the main menu. This was caused by the mod attempting to send network packets after the connection to the server had already closed. Enjoy!

---

## [1.2.0] - 2025-06-11

### Added
- **Jade Integration for Wild Bush Tooltips:** Custom tooltips for Wild Green Bean Bushes and Wild Cucumber Bushes now appear in Jade's overlay if Jade is installed, mirroring their item tooltips.
- **Jade Integration for Hamster Debug Info (Configurable):**
  - Added an advanced debug information overlay for Hamsters when viewed with Jade.
  - Displays detailed AI states (current custom goal, sitting status, navigation, target), sleep sequence phases, love/interaction states, and general info like variant and age.
  - This feature is primarily for debugging and is **disabled by default**.
  - Can be toggled via a new option in the mod's config screen (Mod Menu -> Adorable Hamster Pets -> UI & Quality of Life -> "Enable Jade Hamster Debug Info").
  - Can also be toggled in-game by sneak-right-clicking a tamed hamster while holding the Hamster Guide Book. An action bar message confirms the toggle.
  - This Hamster Debug Info Jade integration is **AWESOME** for myself when bug fixing, but also for anyone reporting bugs (see `README`, "Bug Reporting Etiquette" section).
- **Independent Ore Seeking Feature:**
  - Tamed hamsters, after being on a player's shoulder while a diamond alert was active, can now independently seek out *that* diamond ore upon dismount.
  - Features new AI goal (`HamsterSeekDiamondGoal`) with distinct states: scanning, moving to ore, and waiting if path is blocked.
  - **Whoops! Who Put That There?** (There's a 33% chance a primed hamster targeting diamond might "mistakenly" pathfind to nearby gold ore instead. If this happens the hamster will be shocked and start sulking.)
  - **New Animations:**
    - `anim_hamster_seeking_diamond`: Looping animation for when the hamster is actively moving towards an ore.
    - `anim_hamster_wants_to_seek_diamond`: Looping animation for when the hamster has targeted an ore but its path is blocked.
    - `anim_hamster_sulk` (3s, non-looping) & `anim_hamster_sulking` (looping): Played if the hamster "mistakenly" finds gold.
  - **New Sounds & Effects:**
    - Dust particles (colored like the block the hamster is on) emit from the hamster's nose via animation keyframes while seeking.
    - Upon finding diamond: "Diamond sparkle" sounds play at the ore, special particles (`TRIAL_SPAWNER_DETECTION_OMINOUS` on hamster, `FIREWORKS` above ore) appear, and the hamster plays the begging animation with new "bounce" sound effects triggered by animation keyframes. Aggressive begging sounds also play periodically.
    - Upon "mistakenly" finding gold: A delayed orchestral hit (`alarm_orchestra_hit.ogg`) plays, followed by a delayed "hamster shocked" sound. Smoke particles appear above the gold ore, and black entity effect particles appear on the sulking hamster.
  - **Interaction:** Player right-click clears diamond celebration or sulking states, with new "affection" sounds.
  - **Configuration:** New options in Mod Menu to toggle the feature, its cooldown, and ore scan radius.
  - **Advancement:** New "Canine Aspirations?" advancement for when a hamster first successfully leads to diamond.
- **Jade Debug Overlay Enhancements:** The Jade debug overlay for hamsters now displays new states related to ore seeking (primed status, target ore, cooldown) and the sulking/celebrating diamond states.
- **New Sound Events:** Added `hamster_bounce`, `alarm_orchestra_hit`, `hamster_shocked`, `diamond_sparkle1-3`, and `hamster_affection1-3` sound events and their definitions.
- **Animation Personalities:** Tamed hamsters are now assigned one of three persistent "personalities" at birth, which determines the specific sitting animations they will use for their entire life, making each hamster feel more unique.
- **Configurable Cleaning Frequency:** Added a new option in the config ("Cleaning Frequency") to control how often a sitting hamster will start its cleaning animation.

### Changed
- **Hamster Blinking is Cuter:** Removed the code-based procedural blinking logic for `HamsterEntity`. Eye blinking and closure are now entirely controlled by keyframes within the GeckoLib animations (`anim_hamster.animation.json`), allowing for cuter and more context-aware blinking (e.g., slower blinks when sleepy).
- Updated `README.md` to include instructions for players on how to use and provide the new Jade Hamster Debug Info when reporting bugs.
- **Hamster AI for Following Owner:** Implemented `HamsterFollowOwnerGoal` in order to prevent hamsters from trying to follow their owner while they are celebrating a diamond find or sulking at gold (in addition to existing interruptions like sitting, sleeping, KO).
- **Hamster AI for Looking Around:** `HamsterLookAtEntityGoal` and `HamsterLookAroundGoal` are now disabled when a hamster is knocked out, sitting, or sulking, preventing rotation during this state.
- **Diamond Celebration Behavior:** Hamsters now also use the begging animation when celebrating a diamond find.
- **Begging Animation:** The begging animation has been updated to include keyframe-driven bounce sounds.
- **Dependency Declarations:** Updated `fabric.mod.json` to correctly declare hard dependencies on GeckoLib and owo-lib (now in `depends` section) and list ModMenu and Jade as optional (now in `suggests` section). This improves server compatibility and clarifies requirements for users and modpack creators.
- **Guidebook Update:** Added a new page to the "Hamster Tips" guidebook detailing the "Independent Ore Seeking" feature, including how to prime a hamster for it.
- ### Switched from owo-lib to Fzzy Config, which means lots of awsesome new config features, including change history and the ability to search for specific settings!
- **Footstep Sound System Overhaul:** Hamster footstep sounds now use a hybrid system to improve immersion.
  - When a hamster is on-screen, it uses precise, animation-keyframed sounds that are perfectly synchronized with its footfalls.
  - When a hamster is off-screen (and thus not animating for the client), it uses the traditional vanilla (server-side) movement-based sounds as a fallback. This ensures you can always hear your companions following you.
- **Sound Volume Balancing:**
  - Footstep sounds on gravel are now 40% quieter to better match the volume of sounds on other surfaces like grass and stone.
  - The volume of begging sounds has been reduced.
- **Cleaning Animation Sound:** The cleaning animation now features a continuous, looping scratch sound. Fun fact— it's actually the sound of me scratching my beard 🤭

### Legal
- **License Update & Clarification:** Updated the project license to a split model (see `LICENSE.md`).
  - Creative assets (models, textures, sounds, animations found in `/src/main/resources/`) are now **All Rights Reserved** to better protect the significant artistic effort invested. When "Adorable Hamster Pets" started nearly six months ago as what I thought would be a small weekend project, an MIT license seemed fine. I was so young and innocent of Javascript then. Lol.
  - The Java source code and other non-asset files remain under the **MIT License**, allowing for community learning and code-level contributions.
  - `fabric.mod.json` now points to `LICENSE.md` and specifies "Custom" license type.
  - The `README.md` has been updated with a detailed, user-friendly explanation of these permissions.
  - The `LICENSE` file was renamed to `LICENSE.md` so I could make it more readable.

### Fixed
- **Hamster Guide Book Crafting and NBT Application:**
  - Fixed an issue where the crafting recipe for the Hamster Guide Book (1 Book + 1 Sliced Cucumber) would not appear or unlock correctly.
  - The recipe is now generated via datagen and produces a plain, NBT-less book.
  - A technical advancement now triggers upon crafting this plain book. The function rewarded by this advancement robustly clears any existing guidebooks from the player's inventory and then gives a single, new guidebook pre-filled with all NBT content (pages, title, author), ensuring the player always receives the complete, functional version.
- **Wild Hamster State & Animation Bug on World Load:**
  - Corrected an issue where wild hamsters would incorrectly load with their "Sitting" NBT tag as true, causing them to be stuck in a sitting animation while attempting to wander, and preventing them from sleeping or being tempted correctly.
  - `HamsterEntity` NBT save/load logic now ensures the "Sitting" tag is only saved for tamed, player-commanded sitting, and wild hamsters correctly initialize without a sitting state on load.
- **Hamster Spawning `maxGroupSize` Config Access:** Corrected the code in `ModEntitySpawns.java` to use the proper accessor path for `maxGroupSize` and `spawnWeight` from the owo-lib generated configuration, ensuring these settings are correctly applied during world generation. Due to a confirmed bug with owo-lib, the `maxGroupSize` slider mistakenly displays `0` instead of the default `1`, but this is only visual. Since the mod now uses Fzzy Config, this issue is no longer present.
- **Biome Variant Spawning:** Hamsters now spawn with the correct color variants in all intended biomes. This fixes an issue where hamsters in Stony Shores, Windswept biomes, Jungles, and other previously uncovered areas would incorrectly default to the Orange variant. Black hamsters can now also correctly spawn in the Deep Dark biome.

---

## [1.1.1] - 2025-05-26

### Added
- **Advanced Sleep System for Tamed Hamsters:**
  - Tamed hamsters, when commanded to sit, may now gradually drift off to sleep through a new multi-stage "Path to Slumber" animation sequence.
  - Features new animations: `anim_hamster_drifting_off` (a long, gradual doze), `anim_hamster_settle_sleep1/2/3` (short transitions), and three distinct looping sleep poses (`anim_hamster_sleep_pose1/2/3`).
  - Sleep sequence is influenced by daytime (configurable), nearby threats, and how long they've been sitting.
  - Player interactions (feeding, inventory, etc.) will now wake up a dozing/sleeping tamed hamster.
- **Configurable Sleep Timings:** New options in Mod Menu config to adjust how long tamed hamsters sit before trying to sleep, their threat detection radius for sleep, and whether daytime is required for them to get drowsy.
- **Pink Petal Cycling & Shear Removal:**
  - Right-clicking a tamed hamster with Pink Petals now cycles through three different petal decoration styles.
  - Pink petal decorations can now be removed by right-clicking the hamster with Shears (drops one pink petal).
- **Hamster Water & Fire Avoidance:** Hamsters will now try to pathfind around water, fire, and lava, making them a bit safer. Lol.
- **Shoulder Riding Sounds:**
  - Added new unique sound effects when a hamster mounts the player's shoulder (via cheese interaction).
  - Added a distinct sound effect when a hamster dismounts from the shoulder.
- **New Advancements:**
  - "Petal Pusher": Awarded for decorating a hamster with pink petals. Includes subtle sound/particle effects on first application.
  - "Pocket Paramedic": Awarded when a hamster successfully auto-feeds from its cheek pouch.
  - "Nose for Treasure": Awarded when a shoulder hamster first alerts the player to nearby diamonds.
  - "Impending Doom Squeak": Awarded when a shoulder hamster first alerts the player to a targeting Creeper.
  - "Chipmunk Aspirations": Awarded when a player fills all slots in a hamster's cheek pouch.
- **"Sweet Potato" Easter Egg (Advancement-Based):**
  - Naming a hamster "Sweet Potato" now triggers special effects (sound, particles, message) via a hidden advancement. This is a one-time effect per player. I added this for my wife since the entire mod is based on her real life hamster named Sweet Potato. She doesn't read change logs so she'll never see this. Hehe.

### Changed
- **Hamster Auto-Eating Delay:** Introduced a 2-second delay before a hamster begins to auto-eat from its cheek pouch when injured, giving players a better chance to notice the action and its effects.
- **Hamster Auto-Eating Cooldown:** Increased the cooldown after a successful auto-eat to 3 seconds (60 ticks). It was a bit overpowered.
- **Hamster Melee Attack Particles:** Changed from "poof" particles to "crit" particles, because poof particles already spawn when the attacked entity dies. Let me know If you're reading this and you have ideas for particles I should add to any of the other animations. I'm all ears!
- **Hamster Textures:** Subtle visual enhancement to the area between the hamster's eyes for increased cuteness, especially noticeable during sleep.
- **Configuration Screen Reorganization:** Restructured the Mod Menu config screen with more descriptive top-level sections and logical sub-headers for improved clarity and ease of use. (All your settings are still there, just better organized!)
- **Wild Hamster Sleep Animation:** Wild hamsters now use a new `anim_hamster_wild_settle_sleep` transition animation before entering their looping sleep pose (`anim_hamster_sleep_pose1`).

### Fixed
- **Sliding Sit Bug:** Fixed an issue where tamed hamsters would appear to slide while in their sitting animation if they stood up to defend their owner. Animations now correctly sync with their actual sitting/standing state.
- **Body Rotation While Sitting/Sleeping/KO'd:** Hamsters will no longer rotate their bodies to look at entities while they are in a sitting, sleeping, or knocked-out state.
- **Hamster Targeting:**
  - Tamed hamsters will no longer attack other animals owned by the same player (including horses, wolves, cats, etc.) when commanded by the owner.
  - Tamed hamsters will no longer retaliate against other animals owned by the same player if accidentally hit by them.
- **Shoulder Mounting Sound:** Cheese use sound now plays at the hamster's last location, while the new hamster mount sound plays near the player's ear.

---

## [1.1.0] - 2025-05-19

### Added
- **Advancement Tree:** With distinct branches leading the player to explore different features.
- Custom Advancement Tab ("The Hamster Life"):
  - Guides players through mod features with custom titles, descriptions, and icons.
  - Features a branching structure after initial taming.
  - Includes advancements for obtaining seeds, taming, crafting key items (Sliced Cucumber, Food Mix, Steamed Beans), shouldering, throwing, feeding buffs, and unlocking cheek pouches.
  - Uses custom criteria for specific mod interactions (shouldering, throwing, feeding buffs, pouch unlock, initial guidebook).
  - "Goal" and "Challenge" advancements play distinct sounds on unlock.
- Crafting Recipe for Hamster Guide Book:
  - Players can craft the Hamster Guide Book (1 Vanilla Book + 1 Sliced Cucumber).
  - Crafted book comes pre-filled with all NBT content (pages).
  - A hidden advancement triggers on crafting, playing particle/sound effects.
- Config option `uiTweaks.enableItemTooltips` to toggle custom mod item/block tooltips.
  - When off, tooltips will show item name and "Adorable Hamster Pets" for mod identification.
- Config option `uiTweaks.enableAutoGuidebookDelivery` to toggle automatic guidebook delivery on first join.
  - Thanks to `@MylesGit` on GitHub for suggesting those config ideas, custom advancements, and Guide Book crafting recipe.
- New hamster base color variants: Blue and Lavender.
  - Blue hamsters (with 8 overlay options) spawn rarely in Ice Spikes biomes (70% chance for Blue, 30% for White).
  - Lavender hamsters (with 8 overlay options) spawn rarely in Cherry Grove biomes.
- Four new overlay patterns (overlay5, overlay6, overlay7, overlay8) for all applicable base colors, increasing visual diversity.
- Numerous sound effect variations for hamster actions (idle, hurt, attack, sleep, beg, death, creeper detect, diamond sniff, celebrate).
- Pink Petal cosmetic overlay system:
  - Players can right-click a tamed, owned hamster with `minecraft:pink_petals` to apply one of three random pink petal textures.
  - Right-clicking again with pink petals removes the cosmetic overlay.
  - Applying consumes a petal item; removing does not.
  - Petal state is saved with the hamster and visible on the shoulder.
  - Includes sound and particle effects for application/removal.
- Player-edible Cheese:
  - Cheese is now a food item for players (Nutrition: 8, Saturation: 0.8F).
  - Features a custom eating sound and a faster eating time (20 ticks vs. vanilla 32).
- Cheek Pouch Locking Mechanic:
  - By default, hamster cheek pouches are locked upon taming.
  - Feeding a hamster `HAMSTER_FOOD_MIX` for the first time (resulting in healing/love mode) permanently unlocks its pouches.
  - Plays a sound and spawns particles upon pouch unlock.
  - New config option `features.requireFoodMixToUnlockCheeks` (default: true) allows disabling this lock.
- Display a random, non-repeating message on the action bar when a shoulder hamster dismounts due to sneaking.
  - Configuration option (`features.enableShoulderDismountMessages`) to toggle shoulder dismount messages.

### Changed
- **Hamster Textures:** All hamster textures (including overlays) have been considerably reworked for improved aesthetics and to ensure they are not overly similar to textures from the "Hamsters" mod by Starfish Studios.
- **Hamster Variant System:** Breeding logic updated: If both parents have an overlay, baby must have an overlay, preferably different from parents. If one/neither parent has an overlay, baby can have an overlay (different from parents if applicable) or no overlay.
- **Shoulder Summoning:** Hamsters are now summoned to the shoulder by right-clicking a tamed, owned hamster while holding `ModItems.CHEESE` (instead of right-clicking air with cheese).
- **Hamster Tempting:** Hamsters are now also tempted by (and will beg for) `ModItems.CHEESE` and `ModItems.STEAMED_GREEN_BEANS`, in addition to `ModItems.SLICED_CUCUMBER`.
  - However, `ModItems.SLICED_CUCUMBER` is still the only thing that can tame a hamster.
- **Spawn Egg Item Model:** Now uses data generation (`Models.GENERATED`) for its model, pointing to a custom sprite texture `adorablehamsterpets:item/hamster_spawn_egg` to mesh better with the new spawn egg textures seen in Snapshot 25w08a. (Future proofing.)
- **Item/Creative Tab Icons:** Both the "Adorable Hamster Pets" creative tab and the root "The Hamster Life" advancement tab now use the (custom textured) `HAMSTER_SPAWN_EGG` as their icon.
- **Hamster Spawning:** Removed light level check from spawn restrictions to allow hamsters to spawn in dark areas like caves, provided the block below is valid.
- **Guidebook Content:** Significantly updated and expanded to reflect new features, revised mechanics, and ensure brand voice consistency. Page order adjusted.
- **Guidebook Delivery:** Initial delivery on first join now uses a persistent "flag" advancement (`technical/has_received_initial_guidebook`) to ensure it's truly one-time, respecting the config toggle. The advancement tab (`husbandry/root`) now unlocks immediately for all players via a `minecraft:location` trigger.

### Fixed
- **Melee Attack Particles:** Implemented Geckolib creator, Tslat's suggested fix; particles now spawn correctly at the attacking hamster's foot for all attacks, including the first, and in multi-entity scenarios.
- **Recipe Book Visibility:** Corrected an issue where the "Hamster Food Mix" recipe might not unlock as expected; ensured its advancement criterion correctly requires only `ModItems.SUNFLOWER_SEEDS`.
- Corrected various minor code errors and improved config access patterns.

### Removed
- Numerous hamster sound effect variations used for testing which were not intended to be present in the released version of the mod.
- Shoulder-mounted hamsters no longer dismount when the player takes damage.

---

## [1.0.1] - 2025-05-10

### Changed
- (Internal) Reorganized code structure and updated comments for `HamsterFleeGoal.java`, `HamsterSleepGoal.java`, `HamsterTemptGoal.java`, `HamsterShoulderFeatureRenderer.java`, and `PlayerEntityMixin.java` for improved readability.
- (Internal) Tweaked the "begging" animation to slightly shift the hamster model forward for better visual positioning (`anim_hamster.animation.json`).

### Fixed
- Resolved issue where sitting hamsters could still be tempted by items, causing them to move while sat, which was hilarious. (`HamsterTemptGoal`).
- Corrected wild hamster sleeping behavior: they now consistently wake up when a player approaches, regardless of the player's sneaking status or held item (`HamsterSleepGoal`).
- Adjusted baby hamster rendering (`HamsterModel.java`, `HamsterRenderer.java`):
  - Corrected model scaling logic to properly apply base scales for baby/adult states while allowing JSON animations (e.g., breathing) to function proportionally.
  - Implemented differential scaling for baby hamsters, resulting in a relatively larger head compared to their body.

### Documentation
- Updated `README.md` and item tooltip for the guide book to remove references to an in-book command for re-obtaining it (the book was spawning in without any content and the command to generate its content was way too long for any sane person to type.)

---

## [1.0.0] - 2025-05-04

### Added
- First public version of Adorable Hamster Pets. Hello world!
