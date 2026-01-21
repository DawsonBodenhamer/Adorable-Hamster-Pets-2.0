# -------------------------------------------------------------------
# trigger_book_effects.mcfunction
# Fires when "obtained_guide_book" is granted (fresh obtain only).
# -------------------------------------------------------------------

# 1. Trigger the effects (Sound, Particles, Action Bar) via Java logic
ahp_trigger_guidebook_fx

# 2. Add the state tag to prevent triggering this again while holding the book
tag @s add ahp_has_book