# Google Play listing assets

Generated, do not edit by hand:

- `icon_512.png` - app icon, 512x512, opaque (Play applies its own mask).
- `feature_1024x500.png` - feature graphic.
- `screenshots/*.png` - 1280x800 captures from `:app:snapshot`, usable for phone, 7" and 10" tablets.

Regenerate:

```bash
./gradlew :app:renderIcon -Psvg=assets/icon/A_face.svg -Ppng=build/store/icon_900.png -Psize=900
for c in red_panda fox turtle; do ./gradlew :app:renderIcon -Psvg=assets/characters/$c.svg -Ppng=build/store/$c.png -Psize=600; done
swift tools/store_graphics.swift build/store app/src/commonMain/composeResources/font/nunito.ttf store/play
```

Screenshots: see the `shoot` calls in the git history of this folder, or run `:app:snapshot` with
`-Pscreen`, `-Ptier`, `-Pworld`, `-Phero`, `-Psolve -Pframe=1400`, `-Pmode=predict`.
