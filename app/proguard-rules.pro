# SPDX-License-Identifier: GPL-3.0-or-later
#
# toadCALCULATOR R8 rules.
# Compose, Room and DataStore ship consumer rules; almost nothing is needed here.

# The app has zero reflection-based entry points beyond the Android framework
# itself, so no -keep rules are required.

# Silence harmless notes to keep release logs readable.
-dontwarn java.lang.invoke.StringConcatFactory
