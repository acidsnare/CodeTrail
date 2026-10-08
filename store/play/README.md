# Google Play listing assets

Generated, do not edit by hand:

- `icon_512.png` - app icon, 512x512, opaque (Play applies its own mask).
- `feature_1024x500.png` - feature graphic.
- `screenshots/*.png` - 1280x800 captures from `:app:snapshot`, usable for phone, 7" and 10" tablets (English).
- `screenshots-ru/`, `screenshots-uk/`, `screenshots-da/`, `screenshots-ro/` - the same eight frames in the other store languages.

Regenerate:

```bash
./gradlew :app:renderIcon -Psvg=assets/icon/A_face.svg -Ppng=build/store/icon_900.png -Psize=900
for c in red_panda fox turtle; do ./gradlew :app:renderIcon -Psvg=assets/characters/$c.svg -Ppng=build/store/$c.png -Psize=600; done
swift tools/store_graphics.swift build/store app/src/commonMain/composeResources/font/nunito.ttf store/play
```

Screenshots, one set per language (`lang=en` writes to `screenshots/`):

```bash
lang=ro; dir=screenshots-$lang; [ $lang = en ] && dir=screenshots; mkdir -p store/play/$dir
shoot() { out=$1; shift; ./gradlew :app:snapshot -Plang=$lang -Pstars=120 "$@" -Pout=../store/play/$dir/$out -q; }
shoot 01_menu.png -Pscreen=menu
shoot 02_play.png -Pscreen=play
shoot 03_islands_win.png -Ptier=1 -Pseed=5 -Pworld=islands -Phero=turtle -Psolve -Pframe=1400
shoot 04_forest_jump.png -Ptier=4 -Pseed=9 -Pworld=forest -Phero=fox
shoot 05_lava_loops.png -Ptier=5 -Pseed=7 -Pworld=lava -Phero=red_panda -Psolve -Pframe=1400
shoot 06_space_blocks.png -Ptier=6 -Pseed=4 -Pworld=space -Phero=bear
shoot 07_city_predict.png -Ptier=3 -Pseed=21 -Pworld=city -Phero=penguin -Pmode=predict
shoot 08_stats.png -Pscreen=stats
```
