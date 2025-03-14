package net.gri573.calculator

import kotlin.math.pow
import kotlin.math.roundToInt

enum class TokenType {
    NUMBER, BRACKET, OPERATION, VARIABLE, FUNCTION, SEP
}
enum class OperationType {
    ADD, SUB, MUL, DIV, POW, FACTORIAL
}

data class ExprToken(val tokenType : TokenType, val value : Any?) {
    override fun equals(other: Any?): Boolean {
        return (other is ExprToken && other.tokenType == this.tokenType && other.value == this.value)
    }

    override fun hashCode(): Int {
        var result = tokenType.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}

fun tokenizeExpression(expr : String) : List<ExprToken> {
    val expr2 = expr.replace("**", "^") + " "
    val tokens : MutableList<ExprToken> = mutableListOf()
    var currentTokenType : TokenType? = null
    var currentTokenString = ""
    for (k in expr2.indices) {
        val newType : TokenType? = when (expr2[k]) {
            in "0123456789." -> TokenType.NUMBER
            in " \t\n" -> null
            in "()" -> TokenType.BRACKET
            in "+-*/%^!" -> TokenType.OPERATION
            in ",;" -> TokenType.SEP
            else -> TokenType.VARIABLE
        }
        if (newType == TokenType.OPERATION || newType == TokenType.BRACKET || currentTokenType != newType) {
            if (currentTokenType == TokenType.VARIABLE && expr2[k] == '(') {
                currentTokenType = TokenType.FUNCTION
            }
            if (currentTokenType != null) {
                tokens.add(ExprToken(currentTokenType, when(currentTokenType) {
                    TokenType.NUMBER -> currentTokenString.toDouble()
                    TokenType.BRACKET -> when(currentTokenString) {
                        "(" -> 1
                        ")" -> -1
                        else -> null
                    }
                    TokenType.OPERATION -> when(currentTokenString) {
                        "+" -> OperationType.ADD
                        "-" -> OperationType.SUB
                        "*" -> OperationType.MUL
                        "/" -> OperationType.DIV
                        "^" -> OperationType.POW
                        "!" -> OperationType.FACTORIAL
                        else -> null
                    }
                    TokenType.VARIABLE -> currentTokenString
                    TokenType.FUNCTION -> currentTokenString
                    TokenType.SEP -> null
                }))
            }
            currentTokenString = ""
        }
        currentTokenString += expr2[k]
        currentTokenType = newType
    }
    return tokens.toList()
}

fun flattenBrackets(expr : List<ExprToken>) : List<ExprToken> {
    val expr2 : MutableList<ExprToken> = mutableListOf()
    var bracketDepth = 0
    var lastRootIndex = 0
    for (k in expr.indices) {
        if (
            expr[k].tokenType == TokenType.BRACKET ||
            expr[k].tokenType == TokenType.SEP
        ) {
            if (bracketDepth == 0) {
                lastRootIndex = k
                expr2.add(expr[k])
            }
            when (expr[k].value) {
                1 -> bracketDepth += 1
                -1 -> bracketDepth -= 1
                null -> bracketDepth -= 1
            }
            if (bracketDepth == 0) {
                val replacementToken = ExprToken(
                    TokenType.NUMBER,
                    evaluateExpression(expr.slice(lastRootIndex + 1..<k))
                )
                expr2.add(replacementToken)
                expr2.add(expr[k])
            }
            if (expr[k].value == null) {
                bracketDepth += 1
                lastRootIndex = k
            }
        } else if (bracketDepth == 0) {
            expr2.add(expr[k])
        }
    }
    val expr3 : MutableList<ExprToken> = mutableListOf()
    for (k in expr2.indices) {
        try {
            if (
                (
                    k >= expr2.size-2 ||
                    expr2[k] != ExprToken(TokenType.BRACKET, 1) ||
                    expr2[k + 2] != ExprToken(TokenType.BRACKET, -1)
                ) && (
                    k < 2 ||
                    expr2[k-2] != ExprToken(TokenType.BRACKET, 1) ||
                    expr2[k] != ExprToken(TokenType.BRACKET, -1)
                )
            ) {
                expr3.add(expr2[k])
            }
        } catch (e : IndexOutOfBoundsException) {
            expr3.add(expr2[k])
        }
    }

    return expr3.toList()
}

fun insertVariableValues(expr: List<ExprToken>, variables: Map<String, Double> = variableMap.toMap()) : List<ExprToken> {
    val resultingExpr = expr.toMutableList()
    for (k in resultingExpr.indices) {
        if (resultingExpr[k].tokenType == TokenType.VARIABLE) {
            try {
                val thisExpr = ExprToken(TokenType.NUMBER, variables[resultingExpr[k].value])
                if (thisExpr.value != null) {
                    resultingExpr[k] = thisExpr
                }
            } catch(e : NullPointerException) {}
        }
    }
    return resultingExpr.toList()
}

fun applyFunctions(flatExpr: List<ExprToken>) : List<ExprToken> {
    val resultingExpr : MutableList<ExprToken> = mutableListOf()
    var isToBeSkipped = false
    var skipOnlyNext = false
    for (k in flatExpr.indices) {
        if (
            flatExpr[k].tokenType == TokenType.FUNCTION
        ) {
            val args : MutableList<Double> = mutableListOf()
            if (flatExpr[k+1] == ExprToken(TokenType.BRACKET, 1)) {

                for (
                    lastIndex in k+2..<flatExpr.size-1 step 2
                ) {
                    if (flatExpr[lastIndex].tokenType != TokenType.NUMBER) throw NumberFormatException()
                    try {
                        args.add(flatExpr[lastIndex].value as Double)
                    } catch (e : NullPointerException) {
                        throw NumberFormatException()
                    }
                    if (flatExpr[lastIndex+1] == ExprToken(TokenType.BRACKET, -1)) break
                    if (flatExpr[lastIndex+1].tokenType != TokenType.SEP) throw NumberFormatException()
                }
            } else if (flatExpr[k+1].tokenType == TokenType.NUMBER) {
                try {
                    args.add(flatExpr[k + 1].value as Double)
                } catch (e : NullPointerException) {
                    throw NumberFormatException()
                }
                skipOnlyNext = true
            } else {
                throw NumberFormatException()
            }
            val result : Double
            try {
                result = functionMap[flatExpr[k].value as String]!!.action(args)
            } catch (e : NullPointerException) {
                throw NumberFormatException()
            }
            resultingExpr.add(ExprToken(TokenType.NUMBER, result))
            isToBeSkipped = true
            continue
        }
        if (!isToBeSkipped) {
            resultingExpr.add(flatExpr[k])
        } else {
            isToBeSkipped = flatExpr[k] != ExprToken(TokenType.BRACKET, -1) && !skipOnlyNext
            skipOnlyNext = false
        }
    }
    return resultingExpr.toList()
}

fun applyNegation(flatExpr: List<ExprToken>) : List<ExprToken> {
    val resultingExpr : MutableList<ExprToken> = mutableListOf(flatExpr[0])
    var prevTokenType = TokenType.OPERATION
    for (k in 1..<flatExpr.size) {
        if (
            flatExpr[k-1] == ExprToken(TokenType.OPERATION, OperationType.SUB) &&
            flatExpr[k].tokenType == TokenType.NUMBER &&
            prevTokenType == TokenType.OPERATION
        ) {
            resultingExpr.removeAt(resultingExpr.size-1)
            try {
                resultingExpr.add(ExprToken(TokenType.NUMBER, -(flatExpr[k].value as Double)))
            } catch (e : NullPointerException) {
                throw NumberFormatException()
            }
        } else {
            resultingExpr.add(flatExpr[k])
        }
        prevTokenType = flatExpr[k-1].tokenType
    }
    return resultingExpr.toList()
}

fun applyPow(flatExpr: List<ExprToken>) : List<ExprToken> {
    val resultingExpr : MutableList<ExprToken> = mutableListOf(flatExpr[0])
    var skip = false
    for (k in 1..<flatExpr.size) {
        if (skip) {
            skip = false
            continue
        }
        if (flatExpr[k] == ExprToken(TokenType.OPERATION, OperationType.POW)) {
            if (
                flatExpr[k-1].tokenType != TokenType.NUMBER ||
                flatExpr[k+1].tokenType != TokenType.NUMBER
            ) {
                throw NumberFormatException()
            }
            val prevExprToken = resultingExpr[resultingExpr.size-1]
            resultingExpr.removeAt(resultingExpr.size - 1)
            try {
                resultingExpr.add(
                    ExprToken(
                        TokenType.NUMBER,
                        (prevExprToken.value as Double).pow(flatExpr[k + 1].value as Double)
                    )
                )
            } catch (e : NullPointerException) {
                throw NumberFormatException()
            }
            skip = true
        } else {
            resultingExpr.add(flatExpr[k])
        }
    }
    return resultingExpr.toList()
}

fun applyMulDiv(flatExpr: List<ExprToken>) : List<ExprToken> {
    val resultingExpr : MutableList<ExprToken> = mutableListOf(flatExpr[0])
    var skip = false
    for (k in 1..<flatExpr.size) {
        if (skip) {
            skip = false
            continue
        }
        if (
            flatExpr[k] in setOf(
                ExprToken(TokenType.OPERATION, OperationType.MUL),
                ExprToken(TokenType.OPERATION, OperationType.DIV)
            ) && flatExpr[k-1].tokenType == TokenType.NUMBER &&
            flatExpr[k+1].tokenType == TokenType.NUMBER
        ) {
            val prevExprToken = resultingExpr[resultingExpr.size-1]
            resultingExpr.removeAt(resultingExpr.size-1)
            try {
                resultingExpr.add(
                    ExprToken(
                        TokenType.NUMBER,
                        when (flatExpr[k].value) {
                            OperationType.MUL -> (prevExprToken.value as Double) * (flatExpr[k + 1].value as Double)
                            else -> (prevExprToken.value as Double) / (flatExpr[k + 1].value as Double)
                        }
                    )
                )
            } catch (e : NullPointerException) {
                throw NumberFormatException()
            }
            skip = true
        } else {
            resultingExpr.add(flatExpr[k])
        }
    }
    return resultingExpr.toList()
}

fun applyAddSub(flatExpr: List<ExprToken>) : List<ExprToken> {
    val resultingExpr : MutableList<ExprToken> = mutableListOf(flatExpr[0])
    var skip = false
    for (k in 1..<flatExpr.size) {
        if (skip) {
            skip = false
            continue
        }
        if (
            flatExpr[k] in setOf(
                ExprToken(TokenType.OPERATION, OperationType.ADD),
                ExprToken(TokenType.OPERATION, OperationType.SUB)
            ) && flatExpr[k-1].tokenType == TokenType.NUMBER &&
            flatExpr[k+1].tokenType == TokenType.NUMBER
        ) {
            val prevExprToken = resultingExpr[resultingExpr.size-1]
            resultingExpr.removeAt(resultingExpr.size-1)
            try {
                resultingExpr.add(
                    ExprToken(
                        TokenType.NUMBER,
                        when (flatExpr[k].value) {
                            OperationType.ADD -> (prevExprToken.value as Double) + (flatExpr[k + 1].value as Double)
                            else -> (prevExprToken.value as Double) - (flatExpr[k + 1].value as Double)
                        }
                    )
                )
            } catch (e : NullPointerException) {
                throw NumberFormatException()
            }
            skip = true
        } else {
            resultingExpr.add(flatExpr[k])
        }
    }
    return resultingExpr.toList()
}

fun applyFactorial(flatExpr: List<ExprToken>) : List<ExprToken> {
    val resultingExpr : MutableList<ExprToken> = mutableListOf()
    var skip = false
    for (k in flatExpr.indices.reversed()) {
        if (skip) {
            skip = false
            continue
        }
        if (k >= 1 && flatExpr[k] == ExprToken(TokenType.OPERATION, OperationType.FACTORIAL) && flatExpr[k-1].tokenType == TokenType.NUMBER) {
            var arg = 0
            try {
                arg = (flatExpr[k - 1].value as Double).roundToInt()
            } catch (e : NullPointerException) {
                throw NumberFormatException()
            }
            var res : Long = 1
            for (i in 2..arg) {
                res *= i
            }
            resultingExpr.add(ExprToken(TokenType.NUMBER, value=res.toDouble()))
            skip = true
        } else {
            resultingExpr.add(flatExpr[k])
        }
    }
    return resultingExpr.reversed().toList()
}

fun evaluateExpression(expr : List<ExprToken>) : Double {
    var flatExpr = flattenBrackets(expr)
    flatExpr = insertVariableValues(flatExpr)
    try {
        flatExpr = applyFunctions(flatExpr)
        flatExpr = applyFactorial(flatExpr)
        flatExpr = applyPow(flatExpr)
        flatExpr = applyMulDiv(flatExpr)
        flatExpr = applyNegation(flatExpr)
        flatExpr = applyAddSub(flatExpr)
    } catch(e : IndexOutOfBoundsException) {
        throw  NumberFormatException()
    }
    if (flatExpr.size != 1 || flatExpr[0].tokenType != TokenType.NUMBER) {
        throw NumberFormatException()
    }
    try {
        return flatExpr[0].value as Double
    } catch (e : NullPointerException) {
        throw NumberFormatException()
    }
}