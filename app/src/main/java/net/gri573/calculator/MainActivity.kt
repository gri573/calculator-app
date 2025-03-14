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
    val action : (List<Double>) -> Double,
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
                .replace(Regex(".*[(]"), "")
                .replace(Regex("[)].*"), "").split(Regex(",[ \t]*"))
            fun thisFunction(x : List<Double>) : Double {
                if (inputNames.size != x.size) throw NumberFormatException()
                val inputVarMap : MutableMap<String, Double> = mutableMapOf()
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

val functionMap : MutableMap<String, MathFunction> = mutableStateMapOf(
    "sin"    to MathFunction("sin")    { x: List<Double> -> sin(x[0]) },
    "cos"    to MathFunction("cos")    { x: List<Double> -> cos(x[0]) },
    "tan"    to MathFunction("tan")    { x: List<Double> -> tan(x[0]) },
    "arcsin" to MathFunction("arcsin") { x: List<Double> -> asin(x[0]) },
    "arccos" to MathFunction("arccos") { x: List<Double> -> acos(x[0]) },
    "arctan" to MathFunction("arctan") { x: List<Double> -> atan(x[0]) },
    "sinh"   to MathFunction("sinh")   { x: List<Double> -> sinh(x[0]) },
    "cosh"   to MathFunction("cosh")   { x: List<Double> -> cosh(x[0]) },
    "tanh"   to MathFunction("tanh")   { x: List<Double> -> tanh(x[0]) },
    "asinh"  to MathFunction("asinh")  { x: List<Double> -> asinh(x[0]) },
    "acosh"  to MathFunction("acosh")  { x: List<Double> -> acosh(x[0]) },
    "atanh"  to MathFunction("atanh")  { x: List<Double> -> atanh(x[0]) },
    "exp"    to MathFunction("exp")    { x: List<Double> -> exp(x[0]) },
    "log"    to MathFunction("log")    { x: List<Double> -> when(x.size) {
        1 -> ln(x[0])
        else -> log(x[0], x[1])
    } }
)
val variableMap : MutableMap<String, Double> = mutableStateMapOf(
    "pi" to Math.PI,
    "e" to Math.E
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
        result = evaluateExpression(expr).toString()
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
                        Text(
                            text = "$varName = ${variableMap[varName]}",
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