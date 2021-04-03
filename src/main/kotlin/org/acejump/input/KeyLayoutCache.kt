package org.acejump.input

import org.acejump.config.AceSettings

/**
 * Stores data specific to the selected keyboard layout. We want to assign tags with easily reachable keys first, and ideally have tags
 * with repeated keys (ex. FF, JJ) or adjacent keys (ex. GH, UJ).
 */
internal object KeyLayoutCache {
  /**
   * Stores keys ordered by proximity to other keys for the QWERTY layout.
   * TODO: Support more layouts, perhaps generate automatically.
   */
  private val qwertyCharacterDistances = mapOf(
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
    '0' to "0987654321opiklujmyhntgbrfvedcwsxqaz").mapValues { (_, v) -> v.mapIndexed { index, char -> char to index }.toMap() }
  
  /**
   * Sorts tags according to current keyboard layout settings, and some predefined rules that force tags with digits, and tags with two
   * keys far apart, to be sorted after other (easier to type) tags.
   */
  lateinit var tagOrder: Comparator<String>
    private set
  
  /**
   * Returns all possible two key tags, pre-sorted according to [tagOrder].
   */
  lateinit var allPossibleTags: List<String>
    private set
  
  /**
   * Called before any lazily initialized properties are used, to ensure that they are initialized even if the settings are missing.
   */
  fun ensureInitialized(settings: AceSettings) {
    if (!::tagOrder.isInitialized) {
      reset(settings)
    }
  }
  
  /**
   * Re-initializes cached data according to updated settings.
   */
  fun reset(settings: AceSettings) {
    tagOrder = compareBy(
      { it[0].isDigit() || it[1].isDigit() },
      { qwertyCharacterDistances.getValue(it[0]).getValue(it[1]) },
      settings.layout.priority { it[0] }
    )
    
    val allPossibleChars = settings.allowedChars
      .toCharArray()
      .filter(Char::isLetterOrDigit)
      .distinct()
      .joinToString("")
      .ifEmpty(settings.layout::allChars)
    
    allPossibleTags = allPossibleChars.flatMap { a -> allPossibleChars.map { b -> "$a$b".intern() } }.sortedWith(tagOrder)
  }
}
