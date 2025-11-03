package net.gri573.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.gri573.calculator.ui.theme.CalculatorTheme
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan
import kotlin.math.tanh

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Body(
                        modifier = Modifier.padding(innerPadding).imePadding()
                    )
                }
            }
        }
    }
}

data class MathFunction(
    val description : String,
    val action : (List<List<Double?>>) -> List<Double?>,
)

fun performAssignment(userInput : String) : List<ExprToken>? {
    val logResults = ('\n' in userInput)
    val userInput2 = userInput.replace("\n", "").trim().split("=")
    val name = userInput2[0].split("(")[0].trim()

    val expr : List<ExprToken>
    try {
        expr = tokenizeExpression(userInput2[1])
    } catch (e : NumberFormatException) {
        return null
    }
    if (logResults) {
        if ('(' in userInput2[0]) {
            val inputNames = userInput2[0]
                .replace(Regex("^[^(]*[(]"), "")
                .replace(Regex("[)][^)]*$"), "").split(Regex(",[ \t]*"))
            fun thisFunction(x : List<List<Double?>>) : List<Double?> {
                if (inputNames.size != x.size) throw NumberFormatException()
                val inputVarMap : MutableMap<String, List<Double?>> = mutableMapOf()
                for (k in inputNames.indices) {
                    inputVarMap[inputNames[k]] = x[k]
                }
                val localExpr = insertVariableValues(expr, inputVarMap.toMap())
                return evaluateExpression(localExpr)
            }
            functionMap[name] = MathFunction(userInput.replace("\n", ""), ::thisFunction)
        } else {
            variableMap[name] = evaluateExpression(expr)
        }
    }
    return expr
}

fun Double.sin() : Double { return sin(this) }
fun Double.cos() : Double { return cos(this) }
fun Double.tan() : Double { return tan(this) }
fun Double.asin() : Double { return asin(this) }
fun Double.acos() : Double { return acos(this) }
fun Double.atan() : Double { return atan(this) }
fun Double.sinh() : Double { return sinh(this) }
fun Double.cosh() : Double { return cosh(this) }
fun Double.tanh() : Double { return tanh(this) }
fun Double.asinh() : Double { return asinh(this) }
fun Double.acosh() : Double { return acosh(this) }
fun Double.atanh() : Double { return atanh(this) }
fun Double.exp() : Double { return exp(this) }
fun Double.log(base : Double?) : Double? { return if (base != null) { log(this, base) } else {null}}
fun Double.ln() : Double { return ln(this) }

val functionMap : MutableMap<String, MathFunction> = mutableStateMapOf(
    "sin"    to MathFunction("sin")    { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.sin() }},
    "cos"    to MathFunction("cos")    { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.cos() }},
    "tan"    to MathFunction("tan")    { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.tan() }},
    "arcsin" to MathFunction("arcsin") { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.asin() }},
    "arccos" to MathFunction("arccos") { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.acos() }},
    "arctan" to MathFunction("arctan") { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.atan() }},
    "sinh"   to MathFunction("sinh")   { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.sinh() }},
    "cosh"   to MathFunction("cosh")   { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.cosh() }},
    "tanh"   to MathFunction("tanh")   { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.tanh() }},
    "asinh"  to MathFunction("asinh")  { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.asinh() }},
    "acosh"  to MathFunction("acosh")  { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.acosh() }},
    "atanh"  to MathFunction("atanh")  { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.atanh() }},
    "exp"    to MathFunction("exp")    { x: List<List<Double?>> -> List<Double?>(x[0].size) { x[0][it]?.exp() }},
    "log"    to MathFunction("log")    { x: List<List<Double?>> -> when(x.size) {
        1 -> List<Double?>(x[0].size) { x[0][it]?.ln() }
        else -> List<Double?>(x[0].size) { x[0][it]?.log(x[1][it]) }
    }}
)
val variableMap : MutableMap<String, List<Double?>> = mutableStateMapOf(
    "pi" to List<Double?>(1) {Math.PI},
    "e" to List<Double?>(1) {Math.E}
)



@Composable
fun Body(modifier: Modifier = Modifier) {
    var temporalResult by remember { mutableStateOf("Enter Expression Below") }
    var userInput by remember { mutableStateOf("") }
    var result: String
    var resultColor = Color.Unspecified
    var expr : List<ExprToken> = listOf()
    try {
        expr = tokenizeExpression(userInput)
    } catch (e : NumberFormatException) {
        result = temporalResult
        resultColor = Color.Red
    }

    if ('=' in userInput) {
        try {
            expr = performAssignment(userInput)!!
            if ('\n' in userInput) {
                userInput = ""
            }
        } catch (e : NullPointerException) {
            expr = listOf()
        }
    }
    try {
        val exprResult = evaluateExpression(expr)
        result = when(exprResult.size) {
            1 -> exprResult[0]
            else -> exprResult
        }.toString()
        temporalResult = result
    } catch (e: NumberFormatException) {
        result = temporalResult
        resultColor = Color.Red
    }
    Column(
        modifier
            .fillMaxSize()
    ) {
        Text(
            text = result,
            modifier = Modifier
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
            fontSize = 20.sp,
            color = resultColor
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = userInput,
            colors = TextFieldDefaults.colors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Password
            ),
            onValueChange = {
                userInput = it
            }
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(1.0f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Variables",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    items(variableMap.keys.toList()) { varName ->
                        var value : String
                        if (variableMap[varName]?.size == 1) {
                            value = "%.7g".format(variableMap[varName]!![0])
                        } else if (variableMap[varName] != null) {
                            value = "["
                            for (entry in variableMap[varName]!!) {
                                value += "%.7g, ".format(entry)
                            }
                            value = value.slice(0..value.length-3) + "]"
                        } else {
                            value = "Invalid"
                        }
                        Text(
                            text = "$varName = $value",
                            textAlign = TextAlign.Left,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                        )
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(1.0f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Functions",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    items(functionMap.values.toList()) { function ->
                        FunctionEntry(
                            modifier = Modifier,
                            func = function
                        )
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CalculatorTheme {
        Body(Modifier)
    }
}