/***********************************************************************
 * Copyright (c) 2013-2018 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.tools.utils

class LeafletMapGenerator {
  import LeafletMapGenerator._

  // <head>
  val title = HTMLElement("title", content = "GeoMesa Leaflet Map Viewer")
  val meta = HTMLElement("meta", Some(Map(
    "charset" -> "utf-8",
    "name" -> "viewport",
    "content" -> "width=device-width, initial-scale=1.0"))
  )
  val link = HTMLElement("link", Some(Map(
    "rel" -> "stylesheet",
    "href" -> "https://unpkg.com/leaflet@1.3.1/dist/leaflet.css",
    "integrity" -> "sha512-Rksm5RenBEKSKFjgI3a41vrjkw4EVPlJ3+OiI65vTjIdo9brlAacEuKOiQ5OFh7cOI1bkDwLqdLw3Zg0cRJAAQ==",
    "crossorigin" -> ""))
  )
  val coreLeafletScript = HTMLElement("script", Some(Map(
    "src" -> "https://unpkg.com/leaflet@1.3.1/dist/leaflet.js",
    "integrity" -> "sha512-/Nsx9X4HebavoBvEBuyp3I7od5tA0UzAxs+j83KgC8PU0kgB4XiK4Lfe4y4cgBtaRJQEIFCW+oC506aPT2L1zw==",
    "crossorigin" -> ""))
  )
  val coreHeatmapScript = HTMLElement("script", Some(Map(
    "src" -> "https://cdn.rawgit.com/aheyne/Leaflet.heat/gh-pages/dist/leaflet-heat.js"))
  )
  val style = HTMLElement("style", content =
    """
      |        body {
      |            padding: 0;
      |            margin: 0;
      |        }
      |        html, body, #mapid {
      |            height: 100%;
      |            width: 100%;
      |        }
    """.stripMargin)

  val headChildren = Seq(title, meta, link, coreLeafletScript, coreHeatmapScript, style)
  val head = HTMLElement("head", children = Some(headChildren))
  // </head>

  // <body>
  val mapDiv = HTMLElement("div", Some(Map("id" -> "mapid")))

}

object LeafletMapGenerator {

  case class HTMLElement(tag: String,
                         properties: Option[Map[String, String]] = None,
                         content: String = "",
                         children: Option[Seq[HTMLElement]] = None) {

    def render: String = {
      val str: StringBuilder = new StringBuilder
      val propStr = properties match {
        case Some(props) => props.map(p => s""" ${p._1}="${p._2}"""").mkString("")
        case None => ""
      }
      str.append("<" + tag + propStr + ">")
      children match {
        case Some(childs) => childs.foreach{ child => str.append(child.render) }
        case None =>
      }
      str.append(content)
      str.append("</" + tag + ">")
      str.toString()
    }
  }

  case class JSElement(name: String, value: String)
}
