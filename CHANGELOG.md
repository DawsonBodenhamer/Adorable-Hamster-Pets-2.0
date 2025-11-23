# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.4.0] - 2025-11-13

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
- **Default Compatibility for Croptopia, Immersive Engineering, and Serene Seasons**
  -   Integrated 30 new resource files directly into the mod to ensure out-of-the-box compatibility. A huge thank you to [`@CasualAnimalEnjoyer`](https://github.com/CasualAnimalEnjoyer) for creating and sharing these files!
  -   Cucumbers and Green Beans are now recognized as valid recipe ingredients by **Croptopia**.
  -   Cucumber and Green Bean crops are now categorized by **Serene Seasons**.
  -   Cucumber and Green Bean crops can now be put inside **Immersive Engineering**'s Insulator.

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
