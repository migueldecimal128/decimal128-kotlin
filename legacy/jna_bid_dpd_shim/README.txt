2026-04-14

This code was written a year ago as a bridge to existing Intel libbid
and Colishaw libdecnumber libraries.

It became too hard to maintain once I moved to KMP Kotlin Multiplatform
and began parsing Intel and decTest test vectors.

Therefore, it has been moved under ./legacy

It is a useful example of using jna to attach to the undelying libraries,
perhaps valuable for future QA tests ... at the painful cost of
cross-platform C libraries.

Miguel
