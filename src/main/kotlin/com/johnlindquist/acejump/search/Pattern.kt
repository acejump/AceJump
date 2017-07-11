package com.johnlindquist.acejump.search

enum class Pattern(val string: String) {
  END_OF_LINE("\\n"),
  START_OF_LINE("^.|^\\n"),
  CODE_INDENTS("(?<=^\\s*)\\S|^\\n"),
  LINE_MARK(END_OF_LINE.string + "|" +
    START_OF_LINE.string + "|" +
    CODE_INDENTS.string);

  companion object {
    var adjacent = mapOf(
      // Values are QWERTY keys which are physically adjacent to the map key
      'j' to "jikmnhu", 'f' to "ftgvcdr", 'k' to "kolmji", 'd' to "drfcxse",
      'l' to "lkop", 's' to "sedxzaw", 'a' to "aqwsz",
      'h' to "hujnbgy", 'g' to "gyhbvft", 'y' to "y7uhgt6", 't' to "t6ygfr5",
      'u' to "u8ijhy7", 'r' to "r5tfde4", 'n' to "nbhjm", 'v' to "vcfgb",
      'm' to "mnjk", 'c' to "cxdfv", 'b' to "bvghn",
      'i' to "i9okju8", 'e' to "e4rdsw3", 'x' to "xzsdc", 'z' to "zasx",
      'o' to "o0plki9", 'w' to "w3esaq2", 'p' to "plo0", 'q' to "q12wa",
      '1' to "1qw2", '2' to "21qw3", '3' to "32we4", '4' to "43er5",
      '5' to "54rt6", '6' to "65ty7", '7' to "76yu8", '8' to "87ui9",
      '9' to "98io0", '0' to "09op")
      .mapValues { it.value.toHashSet() }

    var nearby = mapOf(
      // Values are QWERTY keys sorted by physical proximity to the map key
      'j' to "jikmnhuolbgypvftcdrxsezawq",
      'f' to "ftgvcdryhbxseujnzawqikmolp",
      'k' to "kolmjipnhubgyvftcdrxsezawq",
      'd' to "drfcxsetgvzawyhbqujnikmolp",
      'l' to "lkopmjinhubgyvftcdrxsezawq",
      's' to "sedxzawrfcqtgvyhbujnikmolp",
      'a' to "aqwszedxrfctgvyhbujnikmolp",
      'h' to "hujnbgyikmvftolcdrpxsezawq",
      'g' to "gyhbvftujncdrikmxseolzawpq",
      'y' to "yuhgtijnbvfrokmcdeplxswzaq",
      't' to "tygfruhbvcdeijnxswokmzaqpl",
      'u' to "uijhyokmnbgtplvfrcdexswzaq",
      'r' to "rtfdeygvcxswuhbzaqijnokmpl",
      'n' to "nbhjmvgyuiklocftpxdrzseawq",
      'v' to "vcfgbxdrtyhnzseujmawikqolp",
      'm' to "mnjkbhuilvgyopcftxdrzseawq",
      'c' to "cxdfvzsertgbawyhnqujmikolp",
      'b' to "bvghncftyujmxdrikzseolawqp",
      'i' to "iokjuplmnhybgtvfrcdexswzaq",
      'e' to "erdswtfcxzaqygvuhbijnokmpl",
      'x' to "xzsdcawerfvqtgbyhnujmikolp",
      'z' to "zasxqwedcrfvtgbyhnujmikolp",
      'o' to "oplkimjunhybgtvfrcdexswzaq",
      'w' to "wesaqrdxztfcygvuhbijnokmpl",
      'p' to "plokimjunhybgtvfrcdexswzaq",
      'q' to "qwaeszrdxtfcygvuhbijnokmpl",
      '1' to "1234567890",
      '2' to "2134567890",
      '3' to "3241567890",
      '4' to "4352617890",
      '5' to "5463728190",
      '6' to "6574839201",
      '7' to "7685940321",
      '8' to "8796054321",
      '9' to "9807654321",
      '0' to "0987654321")

      .mapValues { it.value.toHashSet() }
  }
}