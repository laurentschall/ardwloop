package org.llschall.ardwloop.serial

import org.llschall.ardwloop.serial.*
import org.llschall.ardwloop.serial.port.*
import org.llschall.ardwloop.structure.*
import org.llschall.ardwloop.structure.data.*
import org.llschall.ardwloop.structure.model.*
import org.llschall.ardwloop.structure.model.keyboard.*
import org.llschall.ardwloop.structure.utils.*
import org.llschall.ardwloop.structure.utils.Logger.err
import org.llschall.ardwloop.structure.utils.Logger.msg
import java.io.StringWriter
import kotlin.collections.set

class Serial internal constructor(private val model: Model, cfg: ProgramCfg, val timer: Timer) {
    private val serialMdl = model.serialMdl
    private var port: ISerialPort? = null
    private var writer: Writer? = null
    private var reader: Reader? = null
    private var connector: Connector? = null

    private val p = cfg.p
    private val rc = cfg.rc
    private val sc = cfg.sc

    @Throws(SerialWriteException::class)
    fun reboot() {
        connector!!.reboot(p, rc, sc)
    }

    @Throws(SerialWriteException::class)
    fun connect(provider: ISerialProvider): Boolean {
        serialMdl.port.name.set("Scanning ...")

        val ports = provider.listPorts()

        if (ports.isEmpty()) {
            serialMdl.status.set("No port found")
            msg("# Serial: No port found !")
            return false
        }

        for (port in ports) {
            val wr = StringWriter()
            msg("# Serial: " + port.descriptivePortName)
            wr.append("# ")
            val arr = arrayOf(
                port.systemPortName,
                port.portDescription,
                port.descriptivePortName,
                "" + port.baudRate,
                "" + port.cTS,
                "" + port.dCD,
                "" + port.deviceReadBufferSize,
                "" + port.deviceWriteBufferSize,
            )
            for (s in arr) {
                wr.append(s)
                wr.append(" ")
            }
            msg(wr.toString())

            val name = port.systemPortName
            val usb = name.contains("USB")
            val rfcomm = name.contains("rfcomm")
            val acm = name.contains("ttyACM")
            val fake = name.contains("FAKE")
            if (usb || rfcomm || acm || fake) {
                this.port = port
                serialMdl.port.name.set(name)
            }
        }

        if (port == null) {
            serialMdl.port.name.set("No valid port found")
            return false
        }

        msg("Serial port => $port")

        serialMdl.port.name.set(port!!.systemPortName)

        val baud = serialMdl.baud.get()
        serialMdl.status.set("Opening...")

        val open = port!!.openPort()

        port!!.baudRate = baud
        msg("BAUD set to " + port!!.baudRate)

        if (!open) {
            err("try sudo chmod 777 /dev/" + port!!.systemPortName)
            serialMdl.status.set("CHMOD")
            return false
        }

        reader = Reader(model, port!!, timer)
        writer = Writer(port!!)

        connector = Connector(model, reader!!, writer!!)

        msg("PLUG")
        connector!!.reboot(p, rc, sc)

        return true
    }

    fun close() {
        if (port != null) {
            port!!.closePort()
            msg(">>> port closed")
        }
        serialMdl.status.set("closed")
    }

    @Throws(SerialLongReadException::class, SerialWrongReadException::class, GotJException::class)
    fun checkP(): SerialData? {
        return reader!!.checkP()
    }

    @Throws(SerialLongReadException::class, SerialWrongReadException::class, GotJException::class)
    fun readS(): SerialData {
        val chk = reader!!.readS()

        var a: SerialVector? = null
        var b: SerialVector? = null
        var c: SerialVector? = null
        var d: SerialVector? = null
        var e: SerialVector? = null
        var f: SerialVector? = null
        var g: SerialVector? = null
        var h: SerialVector? = null
        var i: SerialVector? = null
        for (id in 0 until sc) {
            when (id) {
                0 -> a = reader!!.read('a')
                1 -> b = reader!!.read('b')
                2 -> c = reader!!.read('c')
                3 -> d = reader!!.read('d')
                4 -> e = reader!!.read('e')
                5 -> f = reader!!.read('f')
                6 -> g = reader!!.read('g')
                7 -> h = reader!!.read('h')
                8 -> i = reader!!.read('i')
            }
        }
        return SerialData(
            chk,
            a, b, c, d, e, f, g, h, i
        )
    }

    @Throws(SerialWriteException::class)
    fun writeV(data: SerialData) {
        writer!!.writeR()

        val chars = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i')
        val map: MutableMap<Char, SerialVector?> = HashMap()
        map['a'] = data.a
        map['b'] = data.b
        map['c'] = data.c
        map['d'] = data.d
        map['e'] = data.e
        map['f'] = data.f
        map['g'] = data.g
        map['h'] = data.h
        map['i'] = data.i

        val rc = serialMdl.program.get().rc
        for (i in 0 until rc) {
            val c = chars[i]

            val opt = map[c]
            opt?.let {
                writer!!.write(c, 'v', it.v!!)
                writer!!.write(c, 'w', it.w!!)
                writer!!.write(c, 'x', it.x!!)
                writer!!.write(c, 'y', it.y!!)
                writer!!.write(c, 'z', it.z!!)
            }
        }
        writer!!.flush()
    }

    companion object {
        const val DELAY_BEFORE_K: Int = 99

        const val J: Char = 'J' // ask communication
        const val K: Char = 'K' // init communication
        const val C: Char = 'C' // configuration
        const val S: Char = 'S' // prefix for S message
        const val R: Char = 'R' // prefix for R message
        const val P: Char = 'P' // prefix for P message
        const val N: Char = 'N' // end of chunk
        const val Z: Char = 'Z' // reboot

        const val C_: String = C.toString()
        const val K_: String = K.toString()
        const val J_: String = J.toString()
        const val P_: String = P.toString()
        const val R_: String = R.toString()
        const val Z_: String = Z.toString()
    }
}
