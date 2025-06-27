package org.acejump.input

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.awt.geom.Point2D
import kotlin.math.floor

/**
 * Defines common keyboard layouts. Each layout has a key priority order,
 * based on each key's distance from the home row and how ergonomically
 * difficult they are to press.
 */
@Suppress("unused")
enum class KeyLayout(internal val rows: Array<String>, priority: String) {
  COLEMK(arrayOf("1234567890", "qwfpgjluy", "arstdhneio", "zxcvbkm"), priority = "tndhseriaovkcmbxzgjplfuwyq5849673210"),
  COLEDH(arrayOf("1234567890", "qwfpbjluy", "arstgmneio", "zxcdvkh"), priority = "tngmseriaodkchvxzbjplfuwyq5849673210"),
  WORKMN(arrayOf("1234567890", "qdrwbjfup", "ashtgyneoi", "zxmcvkl"), priority = "tnhegysoaiclvkmxzwfrubjdpq5849673210"),
  DVORAK(arrayOf("1234567890", "pyfgcrl", "aoeuidhtns", "qjkxbmwvz"), priority = "uhetidonasxkbjmqwvzgfycprl5849673210"),
  QWERTY(arrayOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"), priority = "fjghdkslavncmbxzrutyeiwoqp5849673210"),
  QWERTZ(arrayOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"), priority = "fjghdkslavncmbxyrutzeiwoqp5849673210"),
  QGMLWY(arrayOf("1234567890", "qgmlwyfub", "dstnriaeoh", "zxcvjkp"), priority = "naterisodhvkcpjxzlfmuwygbq5849673210"),
  QGMLWB(arrayOf("1234567890", "qgmlwbyuv", "dstnriaeoh", "zxcfjkp"), priority = "naterisodhfkcpjxzlymuwbgvq5849673210"),
  NORMAN(arrayOf("1234567890", "qwdfkjurl", "asetgynioh", "zxcvbpm"), priority = "tneigysoahbvpcmxzjkufrdlwq5849673210"),
  AZERTY(arrayOf("1234567890", "azertyuiop", "qsdfghjklm", "wxcvbn"), priority = "fjghdkslqvncmbxwrutyeizoap5849673210"),
  CANARY(arrayOf("1234567890", "wlypbzfou", "crstgmneia", "qjvdkxh"), priority = "tngmseracidxvhkjqpfbzyoluw5849673210"),
  ENGRAM(arrayOf("1234567890", "byouldwvz", "cieahtsnq", "gxjkrmfp"), priority = "ahetiscnkrjmodulywxfgpbvqz3847295610");

  internal val allChars = rows.joinToString("").toCharArray().apply(CharArray::sort).joinToString("")
  internal val allPriorities = priority.mapIndexed { index, char -> char to index }.toMap()

  private val keyDistances: Map<Char, Object2IntMap<Char>> by lazy {
    val keyDistanceMap = mutableMapOf<Char, Object2IntMap<Char>>()
    val keyLocations = mutableMapOf<Char, Point2D>()

    for ((rowIndex, rowChars) in rows.withIndex()) {
      val keyY = rowIndex * 1.2F // Slightly increase cost of traveling between rows.

      for ((columnIndex, char) in rowChars.withIndex()) {
        val keyX = columnIndex + (0.25F * rowIndex) // Assume a 1/4-key uniform stagger.
        keyLocations[char] = Point2D.Float(keyX, keyY)
      }
    }

    for (fromChar in allChars) {
      val distances = Object2IntOpenHashMap<Char>()
      val fromLocation = keyLocations.getValue(fromChar)

      for (toChar in allChars) {
        distances[toChar] = floor(2F * fromLocation.distanceSq(keyLocations.getValue(toChar))).toInt()
      }

      keyDistanceMap[fromChar] = distances
    }

    keyDistanceMap
  }

  internal inline fun priority(crossinline tagToChar: (String) -> Char): (String) -> Int? {
    return { allPriorities[tagToChar(it)] }
  }

  internal fun distanceBetweenKeys(char1: Char, char2: Char): Int {
    return keyDistances.getValue(char1).getValue(char2)
  }
}
