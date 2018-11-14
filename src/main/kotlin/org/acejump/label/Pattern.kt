package org.acejump.label

import org.acejump.config.AceConfig
import org.acejump.search.mapIndices

/**
 * Patterns related to key priority, separation, and regexps for line mode.
 */

enum class Pattern(val string: String) {
  END_OF_LINE("\\n|\\Z"),
  START_OF_LINE("^.|^\\n"),
  CODE_INDENTS("[^\\s].*|^\\n"),
  LINE_MARK(END_OF_LINE.string + "|" +
    START_OF_LINE.string + "|" +
    CODE_INDENTS.string),
  ALL_WORDS("(?<=[^a-zA-Z0-9_])[a-zA-Z0-9_]");

  companion object {
    private fun distance(fromKey: Char, toKey: Char) = nearby[fromKey]!![toKey]

    private var allBigrams = emptyList<String>()
      get() = AceConfig.settings.allowedChars
        .run { flatMap { e -> map { c -> "$e$c" } } }
        .also { field = it }

    val NUM_TAGS: Int
      get() = NUM_CHARS * NUM_CHARS

    var NUM_CHARS: Int = 36
      get() = AceConfig.settings.allowedChars.length

    val defaultTagOrder: Comparator<String> = compareBy(
      { it[0].isDigit() || it[1].isDigit() },
      { Pattern.distance(it[0], it.last()) },
      AceConfig.settings.layout.priority { it[0] })

    fun filterTags(query: String) = allBigrams.filter { !query.endsWith(it[0]) }

    /**
     * Sorts available tags by key distance. Tags which are ergonomically easier
     * to reach will be assigned first. We would prefer to use tags that contain
     * repeated keys (ex. FF, JJ), and use tags that contain physically adjacent
     * keys (ex. 12, 21) to keys that are located further apart on the keyboard.
     */

    enum class KeyLayout(val text: Array<String>) {
      COLEMK(arrayOf("1234567890", "qwfpgjluy",  "arstdhneio", "zxcvbkm")),
      WORKMN(arrayOf("1234567890", "qdrwbjfup",  "ashtgyneoi", "zxmcvkl")),
      DVORAK(arrayOf("1234567890", "pyfgcrl",    "aoeuidhtns", "qjkxbmwvz")),
      QWERTY(arrayOf("1234567890", "qwertyuiop", "asdfghjkl",  "zxcvbnm")),
      QGMLWY(arrayOf("1234567890", "qgmlwyfub",  "dstnriaeoh", "zxcvjkp")),
      QGMLWB(arrayOf("1234567890", "qgmlwbyuv",  "dntnriaeoh", "zxcfjkp")),
      NORMAN(arrayOf("1234567890", "qwdfkjurl",  "asetgynioh", "zxcvbpm"));

      private val priority
        get() = when (this) {
          QWERTY -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
          // TODO: Prioritize keys by ease of reach for other keyboards
          // TODO: https://github.com/acejump/AceJump/issues/172
          WORKMN -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
          COLEMK -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
          DVORAK -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
          QGMLWB -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
          QGMLWY -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
          NORMAN -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
        }.mapIndices()

      fun chars() = text.flatMap { it.toList() }.sortedBy { priority[it] }

      fun priority(tagToChar: (String) -> Char): (String) -> Int? =
        { priority[tagToChar(it)] }

      fun keyboard() = text.joinToString("\n")
      fun allChars() = chars().joinToString("")
    }

    private val nearby: Map<Char, Map<Char, Int>> = mapOf(
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
      .mapValues { it.value.mapIndices() }
  }
}