package queuesim.backend
import java.util.*
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

typealias Customers = LinkedList<Customer>
fun Customers.lastCustomer() : Customer =
    if (size==0)
        Customer(-1 , 0.0, 0.0 ,0.0 , Server("") , blocked = true)
    else
        last

typealias Servers   = HashSet<Server>
fun Servers.nextFastestIdleServer() =
    sortedBy { it.customers.lastCustomer().departureTime } [0]

data class Customer (val id : Int, val arrivalTime : Double, val interArrivalTime : Double, val serviceTime : Double, val server : Server, val blocked : Boolean = false) {
    val queueWaitingTime : Double
    val serviceStartTime get() = arrivalTime + queueWaitingTime
    val departureTime : Double get() = serviceStartTime + serviceTime
    val systemWaitingTime get() = queueWaitingTime + serviceTime
    init {
        if(!blocked) {
            queueWaitingTime = (server.customers.lastCustomer().departureTime - arrivalTime).coerceAtLeast(0.0)
            server.idleTime += serviceStartTime - server.customers.lastCustomer().departureTime
            server.customers.add(this)
        } else {
            queueWaitingTime = 0.0
        }
    }
    override fun toString() = "$id\t${arrivalTime.asMinutes()}" +
                              if(!blocked) {"\t${serviceStartTime.asMinutes()}\t\t${departureTime.asMinutes()}\t\t${queueWaitingTime.asMinutes()}\t\t${serviceTime.asMinutes()}\t\t${systemWaitingTime.asMinutes()}\t\t\t${server.name}\t\t"}
                              else {"\tBLOCKED"}
}
data class Server(val name : String , val averageServiceRate : Double = 0.0 ) {
    var idleTime = 0.0
    val customers = Customers()
    fun nextServiceTime() = exponentialRandomNumber(averageServiceRate)
}

class TimeFrame(var numberInSystem: Int = 0, var numberInQueue: Int = 0,
                var arrival: Boolean = false, var departure: Boolean = false, var blocked: Boolean = false)

fun SafeArrayList<TimeFrame>.lastTimeFrame() = if(size == 0) TimeFrame() else last()

fun <T> List<T>.forEachInRange(start : Int , end : Int , handler : T.()->Unit) {
    for (i in start..end) {
        get(i).handler()
    }
}

fun Double.fromMinutesToSeconds() = (this*60).roundToInt()

class TimeDomain : SafeArrayList<TimeFrame>({TimeFrame()}) {
    val maximumTimeRegistered get() = size - 1
    var maximumNumberInSystem = 0
    var maximumNumberInQueue = 0
    var totalNumberInSystemPerSecond = 0
    var totalNumberInQueuePerSecond = 0
    fun addEvents(arrivalTime: Double , departureTime: Double , serviceTime: Double) =
        addEvents(arrivalTime.fromMinutesToSeconds() , departureTime.fromMinutesToSeconds() , serviceTime.fromMinutesToSeconds())
    fun addEvents(arrivalTime: Int , departureTime : Int , serviceStartTime : Int) {
        forEachInRange(arrivalTime , departureTime) {
            numberInSystem++
            totalNumberInSystemPerSecond ++
            raiseMaximumNumberInSystem(numberInSystem)
        }
        forEachInRange(arrivalTime , serviceStartTime) {
            numberInQueue++
            totalNumberInQueuePerSecond ++
            raiseMaximumNumberInQueue(numberInQueue)
        }
        putDeparture(departureTime)
        putArrival(arrivalTime)
    }
    fun putBlock(second: Int) {
        get(second).blocked = true
    }
    fun putArrival(second: Int) {
        get(second).arrival = true
    }
    fun putDeparture(second: Int) {
        get(second).departure = true
    }

    private fun raiseMaximumNumberInSystem(number : Int) {
        maximumNumberInSystem = number.coerceAtLeast(maximumNumberInSystem)
    }
    private fun raiseMaximumNumberInQueue(number : Int) {
        maximumNumberInQueue = number.coerceAtLeast(maximumNumberInQueue)
    }
    fun at(minute : Double) = get(minute.fromMinutesToSeconds())

    val averageNumberInSystem get() = totalNumberInSystemPerSecond.toDouble()/maximumTimeRegistered
    val averageNumberInQueue get() = totalNumberInQueuePerSecond.toDouble()/maximumTimeRegistered

    //override val defaultValue get() = if(size>0) get(size-1) else TimeFrame()
}

class QueueSystem(val averageArrivalRate : Double, val servers : Servers, val capacity : Double = Double.POSITIVE_INFINITY) {
    val customers = Customers()
    val timeDomain = TimeDomain()
    var totalNumberServed = 0
    var sumOfQueueWaitingTime = 0.0
    var sumOfSystemWaitingTime = 0.0

    val averageQueueWaitingTime get() = sumOfQueueWaitingTime/totalNumberServed
    val averageSystemWaitingTime get() = sumOfSystemWaitingTime/totalNumberServed
    fun generateCustomer() {
        val interArrivalTime = exponentialRandomNumber(averageArrivalRate)
        val arrivalTime = customers.lastCustomer().arrivalTime + interArrivalTime
        val customer = Customer(nextID() , arrivalTime , interArrivalTime , nextServiceTime(), nextServer(), systemIsFullAt(arrivalTime))
        addCustomer(customer)
    }

    fun addCustomer(customer: Customer) {
        customers.add(customer)
        if(!customer.blocked) {
            sumOfQueueWaitingTime += customer.queueWaitingTime
            sumOfSystemWaitingTime += customer.systemWaitingTime
            totalNumberServed++
            timeDomain.addEvents(customer.arrivalTime , customer.departureTime , customer.serviceStartTime)
        } else {
            timeDomain.putBlock(customer.arrivalTime.fromMinutesToSeconds())
        }
    }

    private fun nextID() = customers.lastCustomer().id+1
    private fun systemIsFullAt(arrivalTime: Double) = timeDomain.at(arrivalTime).numberInSystem >= capacity
    private fun nextServer() = servers.nextFastestIdleServer()
    private fun nextServiceTime() = servers.nextFastestIdleServer().nextServiceTime()
    override fun toString() = "ID\tArrival\tService Start\tDeparture\tTime in Queue\tTime in Service\tTotal Waiting Time\tServer\n" + customers.joinToString("\n")
}

fun exponentialRandomNumber(rate : Double) = ln(1-Math.random()) / -rate

open class SafeArrayList<T>(val defaultValue : SafeArrayList<T>.()->T) : ArrayList<T>() {
    override fun set(index: Int, element: T): T {
        fillUpTo(index)
        return super.set(index, element)
    }
    private fun fillUpTo(index : Int) {
        while(index >= size) {
            add(defaultValue())
        }
    }

    override fun get(index: Int): T {
        fillUpTo(index)
        return super.get(index)
    }
}

fun customerDataStrings() = listOf("ID","Arrival","Service","Departure","Queue Time","Service Time","Total Time","Server")
fun Customer.toStrings() =
    if(blocked) {
        listOf(
            id.toString(),
            arrivalTime.asMinutes(),
            "BLOCKED"
        )
    } else {
        listOf(
            id.toString(),
            arrivalTime.asMinutes(),
            serviceStartTime.asMinutes(),
            departureTime.asMinutes(),
            queueWaitingTime.asMinutes(),
            serviceTime.asMinutes(),
            systemWaitingTime.asMinutes(),
            server.name,
        )
    }


fun Double.asMinutes() = "${toInt().intoTwoDigits()}:${((this-toInt())*60).toInt().intoTwoDigits()}"
fun Double.asMinutesDuration() = asMinutes().replace(":" , "m ") + "s"
fun Int.intoTwoDigits() = if(this<10){"0"}else{""}+this
