# -------------------------------------------------------------------
# clear_book_state.mcfunction
# Fires when "lost_guide_book" is granted (player had tag but lost item).
# Resets the system so effects can play again if they get the book back.
# -------------------------------------------------------------------

# 1. Remove the state tag
tag @s remove ahp_has_book

# 2. Reset both advancements
advancement revoke @s only adorablehamsterpets:technical/obtained_guide_book
advancement revoke @s only adorablehamsterpets:technical/lost_guide_book