package gtkx

import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

public open class Property<T> (private var value: T , onChanged : (oldValue : T , newValue : T)->Unit = {_,_->}) {
    private val changeListeners = LinkedList<(T,T)->Unit>()
    private fun callChangeListeners(old:T , new:T) = changeListeners.forEach{it(old , new)}

    open fun set(newValue : T) {
        val oldValue = value
        value = newValue
        if(oldValue != newValue)
            callChangeListeners(oldValue , newValue)
    }
    open fun get() = value

    init {
        ifChanged(onChanged)
        callChangeListeners(value , value)
    }

    fun ifChanged(handler : (oldValue : T , newValue : T)->Unit) {
        changeListeners.add(handler)
    }
}

private val globalPropertiesMap = HashMap<Any , HashMap<String , Property<Any>>>()
val Any.properties : HashMap<String , Property<Any>>
    get() = globalPropertiesMap.getOrPut(this) {HashMap<String , Property<Any>>()}
//    by lazy {
//        HashMap<String , Property<Any>>()
//    }

fun <T : Any> Any.property(propertyName: String , defaultValue: T  , onChanged : (oldValue : T , newValue : T)->Unit = {_,_->}) : Property<T> =
    try {
        properties.getOrPut(propertyName) { Property(defaultValue , onChanged) as Property<Any> } as Property<T>
    } catch (e : Exception) {
        properties.put(propertyName , Property(defaultValue , onChanged) as Property<Any>)!! as Property<T>
    }
//    if (properties.containsKey(propertyName) && properties.getOrPut(propertyName) { Property(defaultValue , onChanged) }::class == defaultValue::class) {
//        properties.getOrPut(propertyName) { Property(defaultValue , onChanged) } as Property<T>
//    } else {
//        properties.put(propertyName , Property(defaultValue , onChanged))!!
//    }

fun Any.booleanProperty(propertyName: String , onChanged:  (oldValue : Boolean , newValue : Boolean) -> Unit  = {_,_->})   = property(propertyName , false , onChanged)
fun Any.integerProperty(propertyName: String , onChanged:  (oldValue : Int , newValue : Int) -> Unit  = {_,_->})   = property(propertyName , 0     , onChanged)
fun Any.doubleProperty (propertyName: String  , onChanged: (oldValue : Double , newValue : Double) -> Unit =        {_,_->})  = property(propertyName , 0.0   , onChanged)
fun Any.stringProperty (propertyName: String  , onChanged: (oldValue : String , newValue : String) -> Unit =  {_,_->})  = property(propertyName , ""    , onChanged)

fun Property<Boolean>.toggle() {
    set(!get())
}

fun <T , K> Property<T>.follow(property: Property<K> , tranformer: (K)->T) {
    property.ifChanged { _, _ ->
        set(tranformer(property.get()))
    }
}