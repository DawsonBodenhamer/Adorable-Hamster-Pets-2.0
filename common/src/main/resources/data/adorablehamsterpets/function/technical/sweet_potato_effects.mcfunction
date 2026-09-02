# -------------------------------------------------------------------
# sweet_potato_effects.mcfunction  –  Java 1.21.1
# Fires once per player who has JUST named a hamster “Sweet Potato”.
# (All lines are gated by: player must *not* already have the flag.)
# -------------------------------------------------------------------

execute as @s at @s unless entity @s[advancements={adorablehamsterpets:technical/sweet_potato_easter_egg_triggered_flag=true}] run title @s actionbar {"text":"Sweet Potato? An inspired name. Very well.","color":"gold","bold":true}

execute as @s at @s unless entity @s[advancements={adorablehamsterpets:technical/sweet_potato_easter_egg_triggered_flag=true}] run playsound minecraft:entity.firework_rocket.launch player @s ~ ~ ~ 1.0 1.0

execute as @s at @s unless entity @s[advancements={adorablehamsterpets:technical/sweet_potato_easter_egg_triggered_flag=true}] run particle minecraft:happy_villager ~ ~1 ~ 0.5 0.5 0.5 0.1 25 force

execute as @s at @s unless entity @s[advancements={adorablehamsterpets:technical/sweet_potato_easter_egg_triggered_flag=true}] run particle minecraft:flash{color:16777215} ~ ~1 ~ 0.1 0.1 0.1 0.0 10 force

execute as @s at @s unless entity @s[advancements={adorablehamsterpets:technical/sweet_potato_easter_egg_triggered_flag=true}] run advancement grant @s only adorablehamsterpets:technical/sweet_potato_easter_egg_triggered_flag