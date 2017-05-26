package com.johnlindquist.acejump.search

enum class Pattern(val string: String) {
  END_OF_LINE("\\n"),
  START_OF_LINE("^.|^\\n"),
  CODE_INDENTS("(?<=^\\s*)\\S|^\\n"),
  LINE_MARK(END_OF_LINE.string + "|" +
    START_OF_LINE.string + "|" +
    CODE_INDENTS.string);

  companion object {
    fun contains(regex: String) = values().any { it.string == regex }
    val REGEX_PREFIX = ' '
    var adjacent = mapOf(
      // Values are QWERTY keys which are physically adjacent to the map key
      'j' to "jikmnhu", 'f' to "ftgvcdr", 'k' to "kolmji", 'd' to "drfcxse",
      'l' to "lkop", 's' to "sedxzaw", 'a' to "aqwsz",
      'h' to "hujnbgy", 'g' to "gyhbvft", 'y' to "y7uhgt6", 't' to "t6ygfr5",
      'u' to "u8ijhy7", 'r' to "r5tfde4", 'n' to "nbhjm", 'v' to "vcfgb",
      'm' to "mnjk", 'c' to "cxdfv", 'b' to "bvghn",
      'i' to "i9okju8", 'e' to "e4rdsw3", 'x' to "xzsdc", 'z' to "zasx",
      'o' to "o0plki9", 'w' to "w3esaq2", 'p' to "plo0", 'q' to "q12wa")
      .mapValues { it.value.toHashSet() }

    var nearby = mapOf(
      // Values are QWERTY keys sorted by physical proximity to the map key
      'j' to "jikmnhuolbgypvftcdrxsezawq", 'f' to "ftgvcdryhbxseujnzawqikmolp",
      'k' to "kolmjipnhubgyvftcdrxsezawq", 'd' to "drfcxsetgvzawyhbqujnikmolp",
      'l' to "lkopmjinhubgyvftcdrxsezawq", 's' to "sedxzawrfcqtgvyhbujnikmolp",
      'a' to "aqwszedxrfctgvyhbujnikmolp", 'h' to "hujnbgyikmvftolcdrpxsezawq",
      'g' to "gyhbvftujncdrikmxseolzawpq", 'y' to "yuhgtijnbvfrokmcdeplxswzaq",
      't' to "tygfruhbvcdeijnxswokmzaqpl", 'u' to "uijhyokmnbgtplvfrcdexswzaq",
      'r' to "rtfdeygvcxswuhbzaqijnokmpl", 'n' to "nbhjmvgyuiklocftpxdrzseawq",
      'v' to "vcfgbxdrtyhnzseujmawikqolp", 'm' to "mnjkbhuilvgyopcftxdrzseawq",
      'c' to "cxdfvzsertgbawyhnqujmikolp", 'b' to "bvghncftyujmxdrikzseolawqp",
      'i' to "iokjuplmnhybgtvfrcdexswzaq", 'e' to "erdswtfcxzaqygvuhbijnokmpl",
      'x' to "xzsdcawerfvqtgbyhnujmikolp", 'z' to "zasxqwedcrfvtgbyhnujmikolp",
      'o' to "oplkimjunhybgtvfrcdexswzaq", 'w' to "wesaqrdxztfcygvuhbijnokmpl",
      'p' to "plokimjunhybgtvfrcdexswzaq", 'q' to "qwaeszrdxtfcygvuhbijnokmpl")
      .mapValues { it.value.toHashSet() }
  }
}