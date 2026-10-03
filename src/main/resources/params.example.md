# ADAPT parameters file

`Analyse_Batch.readParams()` reads a JSON parameter file (`params.json`). The
top-level value must be an object with `"version": 1`. Any other (or missing)
version is rejected, and every field below is required and type-checked.

See `params.example.json` for a complete, runnable example.

## Migration from the old positional `params.csv`

The old `params.csv` format skipped three header lines and then read 26
whitespace/comma-separated values in a fixed order. A single added or removed
line silently shifted every later value, and the failure surfaced only as a
generic `Exception`.

| Old CSV position | New JSON key | Type |
|---|---|---|
| 1 | `autoThreshold` | boolean |
| 2 | `threshMethod` | string |
| 3 | `greyThresh` | number |
| 4 | `spatialRes` | number |
| 5 | `timeRes` | number |
| 6 | `erosion` | integer |
| 7 | `spatFiltRad` | number |
| 8 | `tempFiltRad` | number |
| 9 | `gaussRad` | number |
| 10 | `genVis` | boolean |
| 11 | `getMorph` | boolean |
| 12 | `analyseProtrusions` | boolean |
| 13 | `blebDetect` | boolean |
| 14 | `curveRange` | integer |
| 15 | `minCurveThresh` | number |
| 16 | `blebLenThresh` | number |
| 17 | `blebDurThresh` | number |
| 18 | `cutOffTime` | number |
| 19 | `cortexDepth` | number |
| 20 | `useSigThresh` | boolean |
| 21 | `sigThreshFact` | number |
| 22 | `sigRecoveryThresh` | number |
| 23 | `minLength` | integer |
| 24 | `filoSizeMax` | number |
| 25 | `getFluorDist` | boolean |
| 26 | `morphSizeMin` | number |

The old parser had commented-out reads for `maxCurveThresh`, `simple`, and
`lambda`; none of these were ever applied, so they have no JSON equivalent.
