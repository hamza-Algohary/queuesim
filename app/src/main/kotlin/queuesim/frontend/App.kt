package queuesim.frontend

import queuesim.backend.QueueSystem
import queuesim.backend.Server
import ch.bailu.gtk.cairo.LineCap
import ch.bailu.gtk.cairo.LineJoin
import ch.bailu.gtk.glib.GlibConstants
import ch.bailu.gtk.gtk.*
import gtkx.application
import gtkx.start
import gtkx.*
import gtkx.Align
import gtkx.Orientation
import gtkx.Overflow
import queuesim.backend.*
import java.io.File
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

fun main() = App().UI()
//{
//    val list = SafeArrayList<Int>(0)
//    println("Size = " + list.size)
//    println(list[5])
//    println("Size = " + list.size)
//    println(list[15])
//
//    for( i in 10..20){
//        list[i] = list[i] +1
//    }
//    println("Size = " + list.size)
//    list.forEach{
//        print("$it ")
//    }
//}
//    10 times {
//        QueueSystem(5.0 , hashSetOf(Server("A",6.0))).apply {
//            1000 times :: generateCustomer
//            //println(this)
//            print("L = "+this.timeDomain.averageNumberInSystem + " ")
//            print("Lq = "+this.timeDomain.averageNumberInQueue + " ")
//            print("W = "+this.averageSystemWaitingTime + " ")
//            print("Wq = "+this.averageQueueWaitingTime + "\n")
//        }
//    }


class App{
    //val servers = hashSetOf(Server("A" , 0.245) , Server("B" , 0.256))
    val calculated = Calculated()

//    var numberOfServers = 2
//    var arrivalRate = 0.1
//    var serviceRate = 0.25

    val showNumberInSystem = Property<Boolean>(true)
    val showNumberInQueue = Property<Boolean>(true)

    val numberOfServers get() = try{getWidgetByID<SpinButton>("numberOfServers").adjustment.value.roundToInt() }catch (e : Exception) {0}
    val arrivalRate get() = try{getWidgetByID<SpinButton>("arrivalRate").adjustment.value}catch (e : Exception) {0.0}
    val serviceRate get() = try{getWidgetByID<SpinButton>("serviceRate").adjustment.value}catch (e : Exception) {0.0}
    val capacity get() = try{getWidgetByID<SpinButton>("capacity").adjustment.value}catch (e : Exception) {0.0}.let { if(it < 1) Double.POSITIVE_INFINITY else it }
    //var system = QueueSystem(arrivalRate, (1..numberOfServers).map{index -> Server(index.toString() , serviceRate)}.toHashSet() )
    var system : QueueSystem = newSystem() //= QueueSystem(arrivalRate , hashSetOf(Server("A",0.25)))
    fun newSystem() = QueueSystem(arrivalRate, (1..numberOfServers).map{index -> Server(index.toString() , serviceRate)}.toHashSet() , capacity)
    fun restart() {
        println("Arrival Rate = $arrivalRate , Service Rate = $serviceRate , Number Of Servers = $numberOfServers , Capacity = $capacity")
        system = newSystem()
    }
    //    return QueueSystem(arrivalRate , hashSetOf(Server("A",0.25)))

    fun tick(numberOfCustomers : Int = 1) {
        if(numberOfServers < 1)
            return
        numberOfCustomers times {system.generateCustomer()}
        calculated.update(arrivalRate , serviceRate , numberOfServers , capacity)
//        println("Total Number In System Per Second= " + system.timeDomain.totalNumberInSystemPerSecond)
//        println("Total Number In Queue Per Second= " + system.timeDomain.totalNumberInQueuePerSecond)
        updateUI()
    }

    fun updateUI() {
        getWidgetByID<UpdatableBin>("table").update()
        getWidgetByID<UpdatableBin>("indicators").update()
        getWidgetByID<DrawingArea>("graph").queueDraw()
    }


    fun UI() {
        globalStyleSheet =
            """.grid-cell {
                border: @window_bg_color solid 1px;
                padding: 5px;
            }
            .grid-header {
                background: lighter(@window_bg_color);
                font-size: 14px;
            }
            """.trimIndent()

        application("algohary.hamza.QueueSimulation") {
            onActivate {
                window("Queue Simulation" , "mainWindow").size(800,600)
                    .titlebar(
                        HeaderBar().cssClasses("flat").start(
                            ToggleButton().icon("open-menu-symbolic").popover(75 , 5) {
                                child(
                                    vbox(5).margins(5).children(
                                        Label("Arrival Rate").halign(Align.START) ,
                                        spinButton(0.01 , 0.01 , Double.MAX_VALUE , 0.01 , 3).id("arrivalRate").onChange { restart() },
                                        Label("Service Rate").halign(Align.START) ,
                                        spinButton(0.01 , 0.01 , Double.MAX_VALUE , 0.01 , 3).id("serviceRate").onChange { restart() },
                                        Label("Number Of Servers").halign(Align.START) ,
                                        spinButton(1.0 , 1.0 , Double.MAX_VALUE , 1.0 , 0).id("numberOfServers").onChange { restart() },
                                        Label("Capacity").halign(Align.START),
                                        spinButton(0.0 , 0.0 , Double.MAX_VALUE , 1.0 , 0).id("capacity").onChange { restart() } ,
                                        hseparator(),
                                        Label("Plot").halign(Align.START),
                                        hbox(1).children(
                                            ToggleButton().hexpand().label("# in system").bind{showNumberInSystem}.active().action { updateUI() }
                                        ),
                                        hbox(1).children(
                                            ToggleButton().hexpand().label("# in queue").bind{showNumberInQueue}.active().action { updateUI() }
                                        )
                                    )
                                )
                            },
                            //Button().icon("media-play").action{tick()},
                            ToggleButton().id("running").icons("media-pause" , "media-play"),//.bind { getWidgetByID<DrawingArea>("graph").animationRunning },
                            ToggleButton().icon("scroll-lock-on").id("autoScroll").tooltips("Auto Scroll").active(true),
                            Button().icon("system-restart-symbolic").action {
                                restart()
                                updateUI()
                            })
                    )
                    .child(
                        vpane()
                            .start(
                                DrawingArea().id("graph").onDraw{width , height ->
                                    settings(color = Color(170,0,0) , lineWidth = 2.0 , lineJoin = LineJoin.ROUND , lineCap = LineCap.ROUND)
                                    val scale = plot(system.timeDomain.mapIndexed{index,frame -> Pair( index.toDouble() , frame.numberInSystem.toDouble())} , width , height , xScale = 10.0 , yScale = 0.4*height/system.timeDomain.maximumNumberInSystem , showNumberInSystem.get())
                                    settings(color = Color(0,0,170) , lineWidth = 2.0 , lineJoin = LineJoin.ROUND , lineCap = LineCap.ROUND)
                                    plot(system.timeDomain.mapIndexed{index,frame -> Pair( index.toDouble() , frame.numberInQueue.toDouble())} , width , height , xScale = 10.0 , yScale = 0.4*height/system.timeDomain.maximumNumberInSystem , showNumberInQueue.get())
                                    settings(color = Color(170,170,170) , lineWidth = 2.0 , lineJoin = LineJoin.ROUND , lineCap = LineCap.ROUND)
                                    plotXAxis(width , height , 100 , scale.first , 0.0 , height/2.0 , textDisplacement = 12.0 , valueRepresenter =  {num->(num/60.0).asMinutes()})
                                    plotYAxis(width , height , 20 , -scale.second , 0.0 , height/2.0 , showAxis = false)
                                }.size(-1 ,100)
                            )
                            .end(
                                hbox(10).children(
                                    vbox().margins(20).cssClasses("card").overflow(Overflow.HIDDEN).children(
                                        hbox().children(*customerDataStrings().map{bold(it)}.toLabels().onEach { it.cssClasses("grid-cell","grid-header").markup(true) }.toTypedArray()),
                                        ScrolledWindow().apply { vadjustment.apply{ onChanged { if(getWidgetByID<ToggleButton>("autoScroll").active) value = upper  }} }
                                            .id("tableScrolledWindow")
                                            .policy(PolicyType.NEVER , PolicyType.AUTOMATIC)
                                            .vexpand()
                                            .child(
                                                UpdatableBin().id("table").child{
                                                    system.customers.subList((system.customers.size-50).coerceAtLeast(0),system.customers.size).tablulate()
                                                }
                                            )
                                    ),
                                    UpdatableBin().id("indicators").cssClasses("card").margins(20).child{
                                        vbox().hexpand().margins(10).children(
                                            title1("Simluation"),
                                            title3("Served  = ${system.totalNumberServed}").halign(Align.START),
                                            title3("L  = ${system.timeDomain.averageNumberInSystem.digits(5)}").halign(Align.START),
                                            title3("Lq = ${system.timeDomain.averageNumberInQueue.digits(5)}") .halign(Align.START),
                                            title3("W  = ${system.averageSystemWaitingTime.asMinutesDuration()}").halign(Align.START),
                                            title3("Wq = ${system.averageQueueWaitingTime.asMinutesDuration()}").halign(Align.START),
                                            hseparator().margins(10),
                                            *calculated.UI(),
                                            hseparator().margins(10),
                                            title1("Idle Times"),
                                            *system.servers.toList().subList(0,numberOfServers).map{server ->
                                                title3("${server.name}  ${( (server.idleTime/server.customers.lastCustomer().departureTime).ifIsNan(1.0)*100).roundToInt()}%").halign(Align.START)
                                            }.toTypedArray()
                                        )
                                    }
                                    ,
                                )
                            )
                    )



                timer(500) {
                    if(getWidgetByID<ToggleButton>("running").active)
                        tick(10)
                    GlibConstants.SOURCE_CONTINUE
                }
            }
        }
    }
}

fun Double.ifIsNan(value : Double) = if(isNaN()) value else this
fun Collection<Customer>.tablulate() =
    map{it.toStrings().toLabels().onEach { it.cssClasses("grid-cell")}}.toGrid()


class Calculated {
    val path = "src/main/resources/python/SystemOutputs.py"
    var model = ""
    var L = 0.0
    var Lq = 0.0
    var W = 0.0
    var Wq = 0.0

    var stable = true
    fun update(averageArrivalRate: Double , averageServiceRate : Double , numberOfServers : Int , capacity : Double = Double.POSITIVE_INFINITY) {
        stable = averageArrivalRate <= averageServiceRate*numberOfServers
        if(!stable)
            return
        try{
            val results = "python3 $path $averageArrivalRate $averageServiceRate $numberOfServers ${if(capacity.isInfinite()){"inf"}else{capacity.roundToInt()} } ".runCommand(File(".")).split(" ")
            model = results[0]
            L  = results[1].toDouble()
            Lq = results[2].toDouble()
            W  = results[3].toDouble()
            Wq = results[4].toDouble()
        } catch (e : Exception) {
            stable = false
            e.printStackTrace()
        }
    }

    fun UI() : Array<Widget> =
        if(stable)
            arrayOf(
                title1("Calculation"),
                title3("L  = ${L.digits(5)}").halign(Align.START),
                title3("Lq = ${Lq.digits(5)}").halign(Align.START),
                title3("W  = ${W.asMinutesDuration()}").halign(Align.START),
                title3("Wq = ${Wq.asMinutesDuration()}").halign(Align.START),
            )
        else
            arrayOf(
                title1("Calculation"),
                title3("System Unstable :(").halign(Align.START)
            )

}


// Copied from https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
fun String.runCommand(workingDir: File): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    proc.waitFor(60, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readText()
}

fun Double.digits(number : Int) = String.format("%."+number+"f",this)

infix fun Int.times(func : ()->Unit) {
    for(i in 0..<this) {
        func()
    }
}