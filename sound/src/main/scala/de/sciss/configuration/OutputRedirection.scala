package de.sciss.configuration

import java.io.{OutputStream, PrintStream}

object OutputRedirection {
  private lazy val instance: Unit = init()

  def apply(): Unit = instance

  private def init(): Unit = {
    val old = System.out
    System.setOut(new MyPrintStream(old, true))
  }

  private final class MyPrintStream(out: PrintStream, flush: Boolean)
    extends PrintStream(out, flush) {

    override def print(s: String): Unit = {
      if (s.equals("JackDriver: exception in real time: alloc failed, increase server's memory allocation (e.g. via ServerOptions)")) {
        // Configuration.controlView.markMemoryExhausted()
        Configuration.killSuperCollider()
        super.print(s)
//      } else if (s.equals("LFGauss_next_a")) {
//        // silently omit -- the problem is there will be a successive new-line...
      } else {
        super.print(s)
      }
    }

//    override def print(obj: scala.Any): Unit = {
//      super.print(obj)
//    }
  }

//  private class OutputStreamImpl(original: PrintStream) extends OutputStream {
//    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
//      // val str = new String(b, off, len)
//      // textPane.append(str)
//      // val foo = b.reverse
//      original.write(b, off, len)
//    }
//
//    private[this] val arr = new Array[Byte](1)
//
//    def write(b: Int): Unit = {
//      arr(0) = b.toByte
//      write(arr, 0, 1)
//    }
//  }
}
