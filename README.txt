Desert Vanguard - Forge 1.20.1 (47.x), JDK 17

1) Textures: already included.
2) Gradle wrapper is not included. Easiest: download the official Forge 1.20.1 MDK,
   copy gradlew, gradlew.bat and the gradle/ folder from it into this project
   (or just copy src/ into the MDK and set mod_id=desertvanguard).
3) ./gradlew runClient   (or ./gradlew build -> build/libs/desertvanguard-1.0.0.jar)
4) Creative -> Spawn Eggs tab -> "Desert Vanguard Spawn Egg",
   or /summon desertvanguard:desert_vanguard

If it faces backwards: set FLIP_FRONT = True in tools/convert_pmx.py, then
  python tools/convert_pmx.py tools/Mob_-_Skeleton_-_Desert_Vanguard.pmx src/main/resources/assets/desertvanguard/models/entity/desert_vanguard.mesh

GitHub build: push the project to a repo (textures included!). Actions tab -> "Build mod"
-> open the finished run -> Artifacts -> desertvanguard-jar. Use the jar WITHOUT "-sources"/"-slim" suffix if several appear.

Pose / animation: the standing pose is the IDLE table in tools/convert_pmx.py (re-run it to regenerate the .mesh).
The walk animation (legs swing) is in DesertVanguardRenderer.java (LEG_SWING = strength). The mob has no AI, so the
legs only move when it is actually moving (e.g. pushed or knocked back).
