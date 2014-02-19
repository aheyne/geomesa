/*
 * Copyright 2013 Commonwealth Computer Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package geomesa.core.iterators

import TimestampRangeIterator._
import collection.JavaConverters._
import java.util.{Date, UUID, Map => JMap}
import org.apache.accumulo.core.client.{IteratorSetting, ScannerBase}
import org.apache.accumulo.core.data.Key
import org.apache.accumulo.core.data.Value
import org.apache.accumulo.core.iterators.IteratorEnvironment
import org.apache.accumulo.core.iterators.SkippingIterator
import org.apache.accumulo.core.iterators.SortedKeyValueIterator

object TimestampRangeIterator {
  def setupIterator(scanner: ScannerBase, startTime: Date, endTime: Date, priority: Int) {
    val iteratorName: String = "tri-" + UUID.randomUUID.toString
    val cfg = new IteratorSetting(priority, iteratorName, classOf[TimestampRangeIterator])
    cfg.addOptions(Map(startOption -> (startTime.getTime / 1000).toString,
                       endOption   -> (endTime.getTime / 1000).toString).asJava)
    scanner.addScanIterator(cfg)
  }

  def setupIterator(scanner: ScannerBase, startTime: Date, endTime: Date) {
    setupIterator(scanner, startTime, endTime, 1)
  }

  var startOption: String = "startOption"
  var endOption: String = "endOption"
}


class TimestampRangeIterator(var start: Long, var end: Long)
    extends SkippingIterator {
  def this() = this(0, Long.MaxValue)

  @Override
  override protected def consume() {
    while (getSource.hasTop && !withinRange(getSource.getTopKey)) {
      getSource.next()
    }
  }

  private def withinRange(topKey: Key): Boolean = {
    topKey.getTimestamp >= start && topKey.getTimestamp <= end
  }

  @Override
  override def deepCopy(env: IteratorEnvironment): SortedKeyValueIterator[Key, Value] = {
    throw new UnsupportedOperationException
  }

  @Override
  override def init(source: SortedKeyValueIterator[Key, Value],
                    options: JMap[String, String],
                    env: IteratorEnvironment) {
    super.init(source, options, env)
    start = options.get(startOption).toLong
    end =   options.get(endOption).toLong
  }
}
