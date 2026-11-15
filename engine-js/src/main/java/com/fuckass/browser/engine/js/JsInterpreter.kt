package com.fuckass.browser.engine.js

sealed interface JsValue {
    data class NumberValue(val value: Double) : JsValue
    data class StringValue(val value: String) : JsValue
    data class BooleanValue(val value: Boolean) : JsValue
    data object NullValue : JsValue
    data object UndefinedValue : JsValue
}

data class JsLimits(
    val maxSourceChars: Int = 64 * 1024,
    val maxTokens: Int = 20_000,
    val maxSteps: Int = 100_000,
    val maxParseDepth: Int = 128
) {
    init {
        require(maxSourceChars > 0 && maxTokens > 0 && maxSteps > 0)
        require(maxParseDepth in 1..1024)
    }
}

data class JsExecutionResult(val value: JsValue, val steps: Int, val variables: Map<String, JsValue>)

/**
 * A first-party, intentionally small JavaScript-like interpreter. It supports primitive values,
 * declarations, assignment, arithmetic, comparisons, if/while and blocks. It has no host-object
 * access, eval, dynamic code loading, functions, prototypes, DOM bindings or modern ECMAScript claim.
 */
class JsInterpreter(private val limits: JsLimits = JsLimits()) {
    fun execute(source: String): JsExecutionResult {
        require(source.length <= limits.maxSourceChars) { "Script exceeds source limit" }
        val parser = Parser(Lexer(source, limits).tokenize(), limits)
        val statements = parser.parseProgram()
        val state = State(limits)
        var last: JsValue = JsValue.UndefinedValue
        statements.forEach { last = it.execute(state) }
        return JsExecutionResult(last, state.steps, state.values.toMap())
    }

    private class State(private val limits: JsLimits) {
        val values = linkedMapOf<String, JsValue>()
        private val constants = mutableSetOf<String>()
        var steps = 0
            private set

        fun tick() {
            steps++
            if (steps > limits.maxSteps) throw IllegalStateException("Script execution budget exceeded")
        }

        fun declare(name: String, value: JsValue, constant: Boolean) {
            tick()
            require(name !in values) { "Identifier already declared: $name" }
            values[name] = value
            if (constant) constants += name
        }

        fun assign(name: String, value: JsValue) {
            tick()
            require(name in values) { "Unknown identifier: $name" }
            check(name !in constants) { "Cannot assign to constant: $name" }
            values[name] = value
        }

        fun get(name: String): JsValue = values[name] ?: throw IllegalArgumentException("Unknown identifier: $name")
    }

    private sealed interface Statement {
        fun execute(state: State): JsValue
    }
    private data class Declaration(val name: String, val expression: Expression?, val constant: Boolean) : Statement {
        override fun execute(state: State): JsValue {
            val value = expression?.evaluate(state) ?: JsValue.UndefinedValue
            state.declare(name, value, constant)
            return value
        }
    }
    private data class Assignment(val name: String, val expression: Expression) : Statement {
        override fun execute(state: State): JsValue {
            val value = expression.evaluate(state)
            state.assign(name, value)
            return value
        }
    }
    private data class ExpressionStatement(val expression: Expression) : Statement {
        override fun execute(state: State): JsValue {
            state.tick()
            return expression.evaluate(state)
        }
    }
    private data class Block(val statements: List<Statement>) : Statement {
        override fun execute(state: State): JsValue {
            var value: JsValue = JsValue.UndefinedValue
            statements.forEach { value = it.execute(state) }
            return value
        }
    }
    private data class IfStatement(
        val condition: Expression,
        val yes: Statement,
        val no: Statement?
    ) : Statement {
        override fun execute(state: State): JsValue {
            state.tick()
            return if (truthy(condition.evaluate(state))) yes.execute(state)
            else no?.execute(state) ?: JsValue.UndefinedValue
        }
    }
    private data class WhileStatement(val condition: Expression, val body: Statement) : Statement {
        override fun execute(state: State): JsValue {
            var value: JsValue = JsValue.UndefinedValue
            while (truthy(condition.evaluate(state))) {
                state.tick()
                value = body.execute(state)
            }
            return value
        }
    }

    private sealed interface Expression {
        fun evaluate(state: State): JsValue
    }
    private data class Literal(val value: JsValue) : Expression {
        override fun evaluate(state: State): JsValue = value
    }
    private data class Identifier(val name: String) : Expression {
        override fun evaluate(state: State): JsValue = state.get(name)
    }
    private data class Unary(val operator: String, val expression: Expression) : Expression {
        override fun evaluate(state: State): JsValue {
            val value = expression.evaluate(state)
            return when (operator) {
                "!" -> JsValue.BooleanValue(!truthy(value))
                "-" -> JsValue.NumberValue(-number(value))
                "+" -> JsValue.NumberValue(number(value))
                else -> error("Unsupported unary operator")
            }
        }
    }
    private data class Binary(val left: Expression, val operator: String, val right: Expression) : Expression {
        override fun evaluate(state: State): JsValue {
            val a = left.evaluate(state)
            if (operator == "&&") return if (truthy(a)) right.evaluate(state) else a
            if (operator == "||") return if (truthy(a)) a else right.evaluate(state)
            val b = right.evaluate(state)
            return when (operator) {
                "+" -> if (a is JsValue.StringValue || b is JsValue.StringValue)
                    JsValue.StringValue(string(a) + string(b))
                else JsValue.NumberValue(number(a) + number(b))
                "-" -> JsValue.NumberValue(number(a) - number(b))
                "*" -> JsValue.NumberValue(number(a) * number(b))
                "/" -> JsValue.NumberValue(number(a) / number(b))
                "%" -> JsValue.NumberValue(number(a) % number(b))
                "<" -> JsValue.BooleanValue(number(a) < number(b))
                ">" -> JsValue.BooleanValue(number(a) > number(b))
                "<=" -> JsValue.BooleanValue(number(a) <= number(b))
                ">=" -> JsValue.BooleanValue(number(a) >= number(b))
                "==", "===", "!=", "!==" -> {
                    val equal = strictEquals(a, b)
                    JsValue.BooleanValue(if (operator == "!=" || operator == "!==") !equal else equal)
                }
                else -> error("Unsupported binary operator")
            }
        }
    }

    private class Lexer(private val source: String, private val limits: JsLimits) {
        private val tokens = mutableListOf<Token>()
        fun tokenize(): List<Token> {
            var i = 0
            while (i < source.length) {
                val c = source[i]
                if (c.isWhitespace()) { i++; continue }
                if (source.startsWith("//", i)) {
                    val end = source.indexOf('\n', i + 2)
                    i = if (end < 0) source.length else end + 1
                    continue
                }
                if (source.startsWith("/*", i)) {
                    val end = source.indexOf("*/", i + 2)
                    require(end >= 0) { "Unterminated comment" }
                    i = end + 2
                    continue
                }
                if (c.isLetter() || c == '_' || c == '$') {
                    val start = i++
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_' || source[i] == '$')) i++
                    add(Token(source.substring(start, i), TokenKind.IDENTIFIER))
                    continue
                }
                if (c.isDigit() || (c == '.' && i + 1 < source.length && source[i + 1].isDigit())) {
                    val start = i++
                    while (i < source.length && (source[i].isDigit() || source[i] == '.')) i++
                    val raw = source.substring(start, i)
                    require(raw.toDoubleOrNull() != null) { "Invalid numeric literal" }
                    add(Token(raw, TokenKind.NUMBER))
                    continue
                }
                if (c == '"' || c == '\'') {
                    val quote = c
                    i++
                    val value = StringBuilder()
                    var closed = false
                    while (i < source.length) {
                        val ch = source[i++]
                        if (ch == quote) { closed = true; break }
                        if (ch == '\\' && i < source.length) {
                            val escaped = source[i++]
                            value.append(when (escaped) { 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; else -> escaped })
                        } else value.append(ch)
                    }
                    require(closed) { "Unterminated string literal" }
                    add(Token(value.toString(), TokenKind.STRING))
                    continue
                }
                val op = OPERATORS.firstOrNull { source.startsWith(it, i) }
                if (op != null) {
                    add(Token(op, TokenKind.SYMBOL))
                    i += op.length
                } else if (c in "(){};,=+-*/%!<>") {
                    add(Token(c.toString(), TokenKind.SYMBOL))
                    i++
                } else {
                    throw IllegalArgumentException("Unsupported character at offset $i")
                }
            }
            add(Token("<eof>", TokenKind.EOF))
            return tokens
        }
        private fun add(token: Token) {
            require(tokens.size < limits.maxTokens) { "Script token limit exceeded" }
            tokens += token
        }
        companion object {
            private val OPERATORS = listOf("===", "!==", "==", "!=", "<=", ">=", "&&", "||")
        }
    }

    private enum class TokenKind { IDENTIFIER, NUMBER, STRING, SYMBOL, EOF }
    private data class Token(val text: String, val kind: TokenKind)

    private class Parser(private val tokens: List<Token>, private val limits: JsLimits) {
        private var index = 0
        private var depth = 0

        fun parseProgram(): List<Statement> {
            val result = mutableListOf<Statement>()
            while (!at("<eof>")) {
                result += statement()
                match(";")
            }
            return result
        }

        private fun statement(): Statement {
            if (match("{")) {
                val body = mutableListOf<Statement>()
                while (!at("}") && !at("<eof>")) {
                    body += statement()
                    match(";")
                }
                expect("}")
                return Block(body)
            }
            if (match("if")) {
                expect("(")
                val condition = expression()
                expect(")")
                val yes = statement()
                val no = if (match("else")) statement() else null
                return IfStatement(condition, yes, no)
            }
            if (match("while")) {
                expect("(")
                val condition = expression()
                expect(")")
                return WhileStatement(condition, statement())
            }
            if (at("let") || at("const") || at("var")) {
                val kind = consume().text
                val name = expectIdentifier()
                val value = if (match("=")) expression() else null
                require(kind != "const" || value != null) { "const requires an initializer" }
                return Declaration(name, value, kind == "const")
            }
            if (peek().kind == TokenKind.IDENTIFIER && peek(1).text == "=") {
                val name = consume().text
                consume()
                return Assignment(name, expression())
            }
            return ExpressionStatement(expression())
        }

        private fun expression(minPrecedence: Int = 0): Expression {
            depth++
            require(depth <= limits.maxParseDepth) { "Expression nesting limit exceeded" }
            try {
                var left = prefix()
                while (true) {
                    val op = peek().text
                    val precedence = PRECEDENCE[op] ?: break
                    if (precedence < minPrecedence) break
                    consume()
                    val right = expression(precedence + 1)
                    left = Binary(left, op, right)
                }
                return left
            } finally {
                depth--
            }
        }

        private fun prefix(): Expression {
            if (at("!") || at("-") || at("+")) return Unary(consume().text, expression(7))
            if (match("(")) {
                val value = expression()
                expect(")")
                return value
            }
            val token = consume()
            return when (token.kind) {
                TokenKind.NUMBER -> Literal(JsValue.NumberValue(token.text.toDouble()))
                TokenKind.STRING -> Literal(JsValue.StringValue(token.text))
                TokenKind.IDENTIFIER -> when (token.text) {
                    "true" -> Literal(JsValue.BooleanValue(true))
                    "false" -> Literal(JsValue.BooleanValue(false))
                    "null" -> Literal(JsValue.NullValue)
                    "undefined" -> Literal(JsValue.UndefinedValue)
                    else -> Identifier(token.text)
                }
                else -> throw IllegalArgumentException("Expected expression")
            }
        }

        private fun expectIdentifier(): String {
            val token = consume()
            require(token.kind == TokenKind.IDENTIFIER) { "Expected identifier" }
            return token.text
        }
        private fun expect(text: String) { require(match(text)) { "Expected '$text'" } }
        private fun match(text: String): Boolean = if (at(text)) { consume(); true } else false
        private fun at(text: String): Boolean = peek().text == text
        private fun peek(ahead: Int = 0): Token = tokens[(index + ahead).coerceAtMost(tokens.lastIndex)]
        private fun consume(): Token = tokens[index++]

        companion object {
            private val PRECEDENCE = mapOf(
                "||" to 1, "&&" to 2, "==" to 3, "!=" to 3, "===" to 3, "!==" to 3,
                "<" to 4, ">" to 4, "<=" to 4, ">=" to 4,
                "+" to 5, "-" to 5, "*" to 6, "/" to 6, "%" to 6
            )
        }
    }

    companion object {
        private fun truthy(value: JsValue): Boolean = when (value) {
            is JsValue.BooleanValue -> value.value
            is JsValue.NumberValue -> value.value != 0.0 && !value.value.isNaN()
            is JsValue.StringValue -> value.value.isNotEmpty()
            JsValue.NullValue, JsValue.UndefinedValue -> false
        }
        private fun number(value: JsValue): Double = when (value) {
            is JsValue.NumberValue -> value.value
            is JsValue.BooleanValue -> if (value.value) 1.0 else 0.0
            JsValue.NullValue -> 0.0
            is JsValue.StringValue -> value.value.toDoubleOrNull() ?: Double.NaN
            JsValue.UndefinedValue -> Double.NaN
        }
        private fun string(value: JsValue): String = when (value) {
            is JsValue.StringValue -> value.value
            is JsValue.NumberValue -> value.value.toString()
            is JsValue.BooleanValue -> value.value.toString()
            JsValue.NullValue -> "null"
            JsValue.UndefinedValue -> "undefined"
        }
        private fun strictEquals(a: JsValue, b: JsValue): Boolean = when {
            a is JsValue.NumberValue && b is JsValue.NumberValue -> a.value == b.value
            a is JsValue.StringValue && b is JsValue.StringValue -> a.value == b.value
            a is JsValue.BooleanValue && b is JsValue.BooleanValue -> a.value == b.value
            a === JsValue.NullValue && b === JsValue.NullValue -> true
            a === JsValue.UndefinedValue && b === JsValue.UndefinedValue -> true
            else -> false
        }
    }
}
