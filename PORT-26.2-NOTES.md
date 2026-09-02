# Adorable Hamster Pets — Minecraft 26.2 / Fabric port notes

Branch `port/26.2` ports the mod from 1.21.1 to **Minecraft 26.2** (Fabric Loader 0.19.5,
Fabric API 0.159.0+26.2). It was done for a private, non-commercial server; it is offered
upstream as-is so the maintainer can pick whatever is useful. Code changes are under the
repository's LGPL v3; no textures, models, sounds or other ARR assets were altered.

## Toolchain

- Minecraft 26.2 ships deobfuscated, so there are no Yarn/Mojang mappings any more.
  The build uses `dev.architectury.loom-no-remap` 1.17-SNAPSHOT, Gradle 9.5.1, JDK 25
  (`org.gradle.java.installations.paths` in `gradle.properties`), Shadow 9.2.2.
- All Java sources were converted from Yarn names to Mojang names (stage 1 of the history).
- Dependencies with 26.2 builds: Architectury 21.0.7, GeckoLib 5.5.4, Fzzy Config 0.7.6+26.2,
  Jade 26.2.11, Mod Menu 20.0.1, Fabric Language Kotlin 1.13.13.
- The **NeoForge** subproject is disabled in `settings.gradle` (no 26.2 NeoForge toolchain was
  attempted). Only `:common` and `:fabric` build.
- **Datagen is disabled**; the 1.21.1 datagen output was committed under
  `common/src/main/resources` and converted to 26.2 formats by hand (see below).

## Vanilla API changes handled (highlights)

- Render pipeline: `MultiBufferSource`/`GuiGraphics`/`ItemRenderer` are gone. Renderers extract
  state first and draw later (`SubmitNodeCollector`, `EntityRenderState`, `GuiGraphicsExtractor`).
  `HamsterRenderer` is now `GeoEntityRenderer<HamsterEntity, HamsterRenderState>`; shoulder
  hamsters are drawn from a `RenderLayer<AvatarRenderState, PlayerModel>` fed by a mixin on
  `AvatarRenderer.extractRenderState` (the way vanilla draws shoulder parrots).
- GeckoLib 5: `GeoRenderState` data tickets, `BoneSnapshot` for per-frame bone changes,
  resources moved to `assets/<ns>/geckolib/models|animations` with suffix-less ids,
  `AnimationController(String, int, handler)`. Custom render states that implement
  `GeoRenderState` must also override `addGeckolibData/hasGeckolibData/getGeckolibData`
  (GeckoLib mixes its own map into the vanilla render state).
- Entities: `ValueInput/ValueOutput` save data (bridged through a single `CompoundTag`),
  `EntityReference` owners, `EntitySpawnReason`, `hurtServer`, `snapTo`, `TEMPT_RANGE`
  attribute (required by `TemptGoal`), Fabric's `FabricEntityDataRegistry` for the custom
  `CompoundTag` entity-data serializer.
- Items/blocks: `Item.Properties.setId`/`BlockBehaviour.Properties.setId` are mandatory,
  `useBlockDescriptionPrefix`/`useItemDescriptionPrefix`, new `appendHoverText` signature,
  `SpawnEggItem(Properties.spawnEgg(type))`, `CropBlock` subclasses cannot copy `Blocks.WHEAT`
  properties (state lambdas reference the vanilla `AGE`), `BushBlock` → `VegetationBlock`,
  `onRemove` → `BlockEntity.preRemoveSideEffects`, `RenderShape.INVISIBLE` for GeckoLib blocks.
- Screens: `AbstractContainerScreen` extract methods, `MouseButtonEvent`/`KeyEvent`/`CharacterEvent`,
  `Minecraft.gui.screen()`/`setScreen`, `MenuScreens.register` is private (registered through
  reflection in `ClientScreenRegistration`), `KeyMapping.Category`, `FontDescription`,
  `ClickEvent` records, `Screen.hasShiftDown` → GLFW query.
- Commands/permissions: `permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`.
- Advancements: `advancements.predicates`/`advancements.triggers` packages; JSON entity
  predicates use `minecraft:entity_type`.
- Worldgen: `minecraft:random_patch` no longer exists; patches are `simple_block` features
  spread by the placed feature (`count` + `random_offset` + `block_predicate_filter`), matching
  vanilla 26.2 flower patches.
- Data formats: recipes use string ingredients, client item definitions in
  `assets/<ns>/items/*.json` (GeckoLib items use `minecraft:special` + `geckolib:geckolib`),
  vanilla enchantment overrides rebased on the 26.2 JSON, `flash` particle needs `color`.
- Mixins: all `@Inject` targets renamed to Mojang names; Jade providers split into
  server-data and client-component classes (Jade 1.21.6+ rule); `AHPJadePlugin` keeps client
  registrations in a separate class so dedicated servers never load client classes.
- Colours: 26.2 text rendering honours alpha, so packed colours must be opaque.

## Removed or stubbed

- Trinkets / Accessories integration (no 26.2 builds). `AcornRingUtilImpl` reports no optional slots.
- Patchouli guide book: Patchouli has no official 26.2 build. The integration compiles against
  the published 26.1 API and runs with an unofficial 26.2 port of Patchouli 26.1-94
  (branch `port/26.2` of a Patchouli fork; multiblock ghost rendering is disabled there).
  Without Patchouli installed the book item is inert.

## Restored on 26.2 (second pass)

- Mouth item and bone-locked riders: drawn through GeckoLib 5 per-bone render tasks
  (`RenderPassInfo.addPerBoneRender`) with `ItemStackRenderState`/entity `submit`; vanilla's own
  rider pass is skipped by a mixin on `EntityRenderDispatcher.shouldRender`.
- Block jiggle (tree heist): re-implemented on Fabric `LevelRenderEvents.COLLECT_SUBMITS` with
  `SubmitNodeCollector.submitMovingBlock`.
- Cheese chewing sound via the `Consumable` component; hamster armor enchantability via
  `Item.Properties.enchantable`.

## Verified

- Full build green; dedicated server boots with a migrated Paper 26.2 world; client joins,
  hamsters spawn/render/animate, inventory GUI, shoulder riding, beds, guide book, Jade
  tooltips, creative tab; no errors in client or server logs during play sessions.
- Not exercised: NeoForge, datagen, Trinkets/Accessories paths, multiblock pages.
