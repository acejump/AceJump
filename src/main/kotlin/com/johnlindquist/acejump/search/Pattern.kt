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
      'j' to "jikmnhuolbgypvftcdrxsezawq8796054321",
      'f' to "ftgvcdryhbxseujnzawqikmolp5463728190",
      'k' to "kolmjipnhubgyvftcdrxsezawq9807654321",
      'd' to "drfcxsetgvzawyhbqujnikmolp4352617890",
      'l' to "lkopmjinhubgyvftcdrxsezawq0987654321",
      's' to "sedxzawrfcqtgvyhbujnikmolp3241567890",
      'a' to "aqwszedxrfctgvyhbujnikmolp1234567890",
      'h' to "hujnbgyikmvftolcdrpxsezawq6758493021",
      'g' to "gyhbvftujncdrikmxseolzawpq5647382910",
      'y' to "yuhgtijnbvfrokmcdeplxswzaq6758493021",
      't' to "tygfruhbvcdeijnxswokmzaqpl5647382910",
      'u' to "uijhyokmnbgtplvfrcdexswzaq7869504321",
      'r' to "rtfdeygvcxswuhbzaqijnokmpl4536271890",
      'n' to "nbhjmvgyuiklocftpxdrzseawq7685940321",
      'v' to "vcfgbxdrtyhnzseujmawikqolp5463728190",
      'm' to "mnjkbhuilvgyopcftxdrzseawq8970654321",
      'c' to "cxdfvzsertgbawyhnqujmikolp4352617890",
      'b' to "bvghncftyujmxdrikzseolawqp6574839201",
      'i' to "iokjuplmnhybgtvfrcdexswzaq8970654321",
      'e' to "erdswtfcxzaqygvuhbijnokmpl3425167890",
      'x' to "xzsdcawerfvqtgbyhnujmikolp3241567890",
      'z' to "zasxqwedcrfvtgbyhnujmikolp1234567890",
      'o' to "oplkimjunhybgtvfrcdexswzaq9087654321",
      'w' to "wesaqrdxztfcygvuhbijnokmpl2314567890",
      'p' to "plokimjunhybgtvfrcdexswzaq0987654321",
      'q' to "qwaeszrdxtfcygvuhbijnokmpl1234567890",
      '1' to "1234567890qawzsexdrcftvgybhunjimkolp",
      '2' to "2134567890qwasezxdrcftvgybhunjimkolp",
      '3' to "3241567890weqasdrzxcftvgybhunjimkolp",
      '4' to "4352617890erwsdftqazxcvgybhunjimkolp",
      '5' to "5463728190rtedfgywsxcvbhuqaznjimkolp",
      '6' to "6574839201tyrfghuedcvbnjiwsxmkoqazlp",
      '7' to "7685940321yutghjirfvbnmkoedclpwsxqaz",
      '8' to "8796054321uiyhjkotgbnmlprfvedcwsxqaz",
      '9' to "9807654321ioujklpyhnmtgbrfvedcwsxqaz",
      '0' to "0987654321opiklujmyhntgbrfvedcwsxqaz")
      .mapValues { it.value.toHashSet() }
  }
}