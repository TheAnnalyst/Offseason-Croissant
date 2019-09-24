package frc.robot.subsystems.superstructure

import com.ctre.phoenix.CANifier
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.commands.FalconSubsystem
import org.ghrobotics.lib.mathematics.units.SIUnit
import org.ghrobotics.lib.mathematics.units.Second
import org.ghrobotics.lib.mathematics.units.millisecond
import org.ghrobotics.lib.mathematics.units.second
import org.team5940.pantry.lib.FishyRobot
import java.awt.Color

object LEDs: FalconSubsystem() {

    override fun lateInit() {
        updateThread.start()
    }

    fun setVisionMode(wantsVision: Boolean) {
        this.wantedState = if(wantsVision) {
            State.Blink((1.0/8.0).second, Color.green)
        } else {
            State.Solid(Color.green)
        }
    }

    sealed class State(open val color: Color) {
        open class Solid(override val color: Color): State(color)
        object Default: Solid(Color.red)
        object Off: Solid(Color.black)
        class Blink(val blinkTime: SIUnit<Second>, override val color: Color, val count: Int = -1, val nextState: State = Off): State(color)
        class Fade(val fadeTime: SIUnit<Second>, override val color: Color): State(color)
    }

    var wantedState: State = State.Default
        @Synchronized get
        @Synchronized set

    var lastWantedState = wantedState
        @Synchronized get
        @Synchronized set

    private val updateThread = FishyRobot.updateScope.launch {
        while(true) {
            when(val wantedState = this@LEDs.wantedState) {
                is State.Solid -> { setColor(wantedState.color); delay(250) }
                is State.Blink -> {
                    if(wantedState.count < 0) {
                        setColor(wantedState.color)
                        delay(wantedState.blinkTime.millisecond.toLong() / 2)
                        setColor(Color.BLACK)
                        delay(wantedState.blinkTime.millisecond.toLong() / 2)
                    } else {
                        for(i in 0..wantedState.count) {
                            if(this@LEDs.wantedState != this@LEDs.lastWantedState) break
                            setColor(wantedState.color)
                            delay(wantedState.blinkTime.millisecond.toLong() / 2)
                            setColor(Color.BLACK)
                            delay(wantedState.blinkTime.millisecond.toLong() / 2)
                        }
                        this@LEDs.wantedState = wantedState.nextState
                    }
                }
                is State.Fade -> {
                    val startColor = lastWantedState.color
                    val endColor = wantedState.color
                    val delta = endColor - startColor
                    // we can do a subdivision every, say, 20ms
                    // so we divide the total duration by 20ms to get how long we have
                    val steps = (wantedState.fadeTime.millisecond / 20.0).toInt()
                    for(i in 0..steps) {
                        val interpolated = startColor + delta * (i.toDouble() / steps.toDouble())
                        setColor(interpolated)
                        delay(20)
                    }
                    setColor(endColor)
                }
            }
            lastWantedState = wantedState
        }
    }

    operator fun Color.plus(other: Color) = Color(this.red + other.red, this.green + other.green, this.blue + other.blue)
    operator fun Color.times(scalar: Double) = Color((this.red * scalar).toFloat(), (this.green * scalar).toFloat(), (this.blue * scalar).toFloat())
    operator fun Color.minus(other: Color) = Color(this.red - other.red, this.green - other.green, this.blue - other.blue)

    private val canifier by lazy { Proximal.canifier }
    fun setColor(color: Color) {
//        println("Setting color to r${color.red} r${color.green} b${color.blue}")
        canifier.setLEDOutput(color.red * (1.0 / 255.0), CANifier.LEDChannel.LEDChannelB)
        canifier.setLEDOutput(color.green * (1.0 / 255.0), CANifier.LEDChannel.LEDChannelA)
        canifier.setLEDOutput(color.blue * (1.0 / 255.0), CANifier.LEDChannel.LEDChannelC)
    }

    val PURPLE = Color(128, 0, 128)
}