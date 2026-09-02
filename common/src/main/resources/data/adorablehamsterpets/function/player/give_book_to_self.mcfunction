# This function is executed by the server AS the player who ran the command link.
# It gives the player one instance of the custom guide book.
give @s adorablehamsterpets:hamster_guide_book[patchouli:book="adorablehamsterpets:hamster_tips_guide_book"] 1

# --- Play Sound and Particle Effects ---
playsound minecraft:block.enchantment_table.use player @s ~ ~ ~ 0.5 1.2
playsound minecraft:item.book.page_turn player @s ~ ~ ~ 0.7 1.5
particle minecraft:enchant ~ ~1 ~ 0.3 0.5 0.3 0.05 50 force @s
particle minecraft:happy_villager ~ ~1 ~ 0.5 0.5 0.5 0.02 20 force @s

# --- Send Action Bar Message ---
title @s actionbar {"text":"A wealth of hamster knowledge, rediscovered.","color":"gold"}