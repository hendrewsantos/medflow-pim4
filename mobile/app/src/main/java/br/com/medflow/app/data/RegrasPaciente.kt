package br.com.medflow.app.data

/** CPF: 11 dígitos e dígitos verificadores corretos (mesma regra da API). */
object Cpf {
    fun somenteDigitos(texto: String): String = texto.filter { it.isDigit() }

    fun valido(texto: String): Boolean {
        val d = somenteDigitos(texto)
        if (d.length != 11 || d.all { it == d[0] }) return false
        fun digito(qtd: Int): Int {
            val soma = (0 until qtd).sumOf { (d[it] - '0') * (qtd + 1 - it) }
            val resto = soma % 11
            return if (resto < 2) 0 else 11 - resto
        }
        return digito(9) == d[9] - '0' && digito(10) == d[10] - '0'
    }

    /** 52998224725 -> 529.982.247-25 (para exibir enquanto a pessoa digita). */
    fun formatar(texto: String): String {
        val d = somenteDigitos(texto).take(11)
        return buildString {
            d.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append('.')
                if (i == 9) append('-')
                append(c)
            }
        }
    }
}

object SenhaRegras {
    /** Ao menos 8 caracteres, com letras e números (mesma regra da API). */
    fun forte(senha: String): Boolean = senha.length >= 8 && senha.any { it.isLetter() } && senha.any { it.isDigit() }
}

/**
 * Pontos de atenção entre medicamentos de uma mesma Box. Tabela de demonstração baseada em orientações de bula:
 * informa e orienta, nunca bloqueia o pedido, e a validação final é da farmacêutica responsável.
 */
object Interacoes {
    data class Aviso(val titulo: String, val texto: String)

    private val grupos = mapOf(
        "levotiroxina" to listOf("levotiroxina"),
        "ibp" to listOf("omeprazol", "pantoprazol", "esomeprazol", "lansoprazol"),
        "calcioFerro" to listOf("carbonato de cálcio", "sulfato ferroso"),
        "sinvastatina" to listOf("sinvastatina"),
        "anlodipino" to listOf("anlodipino"),
        "sistemaRenina" to listOf("losartana", "valsartana", "olmesartana", "candesartana", "enalapril", "captopril", "ramipril", "lisinopril"),
        "potassio" to listOf("espironolactona", "cloreto de potássio"),
        "aas" to listOf("ácido acetilsalicílico", "aas "),
        "antitromboticos" to listOf("varfarina", "clopidogrel"),
        "varfarina" to listOf("varfarina"),
        "amiodarona" to listOf("amiodarona"),
    )

    private data class Regra(val a: String, val b: String, val aviso: Aviso)

    private val regras = listOf(
        Regra("levotiroxina", "ibp", Aviso("Levotiroxina com inibidor de bomba de prótons",
            "Omeprazol e similares podem reduzir a absorção da levotiroxina. A farmacêutica avalia se a dose da tireoide precisa de acompanhamento.")),
        Regra("levotiroxina", "calcioFerro", Aviso("Levotiroxina com cálcio ou ferro",
            "Cálcio e ferro atrapalham a absorção da levotiroxina quando tomados juntos. Costuma-se separar por cerca de 4 horas; confira os horários.")),
        Regra("sinvastatina", "anlodipino", Aviso("Sinvastatina com anlodipino",
            "O anlodipino aumenta a exposição à sinvastatina. A bula recomenda limitar a sinvastatina a 20 mg por dia nessa combinação.")),
        Regra("sistemaRenina", "potassio", Aviso("Losartana e similares com espironolactona ou potássio",
            "A combinação pode elevar o potássio no sangue. Pode ser preciso exame de acompanhamento.")),
        Regra("aas", "antitromboticos", Aviso("AAS com varfarina ou clopidogrel",
            "Usados juntos, aumentam o risco de sangramento. Só devem seguir juntos com indicação médica.")),
        Regra("varfarina", "amiodarona", Aviso("Varfarina com amiodarona",
            "A amiodarona aumenta o efeito da varfarina e exige acompanhamento do INR.")),
    )

    private fun pertence(texto: String, grupo: String) = grupos.getValue(grupo).any { texto.contains(it) }

    /** Recebe "nome princípio ativo" de cada item da Box e devolve os pontos de atenção encontrados. */
    fun de(itens: List<String>): List<Aviso> {
        val textos = itens.map { " ${it.lowercase()} " }
        return regras.filter { r ->
            val a = textos.filter { pertence(it, r.a) }
            val b = textos.filter { pertence(it, r.b) && it !in a }
            a.isNotEmpty() && b.isNotEmpty()
        }.map { it.aviso }
    }
}

/** Programas de cuidado, para organizar o catálogo (a mesma ideia do site). */
object Programas {
    val todos = listOf("Hipertensão", "Diabetes", "Colesterol", "Tireoide", "Estômago", "Vitaminas e minerais", "Coração e circulação", "Outros cuidados")

    private val porPrincipio: Map<String, String> = buildMap {
        fun add(programa: String, vararg ativos: String) = ativos.forEach { put(it, programa) }
        add("Hipertensão", "losartana potássica", "besilato de anlodipino", "atenolol", "hidroclorotiazida", "maleato de enalapril", "captopril", "ramipril",
            "lisinopril", "valsartana", "olmesartana medoxomila", "candesartana cilexetila", "carvedilol", "succinato de metoprolol", "cloridrato de propranolol",
            "fumarato de bisoprolol", "espironolactona", "clortalidona", "indapamida", "furosemida", "cloridrato de hidralazina")
        add("Diabetes", "cloridrato de metformina", "cloridrato de metformina de liberação prolongada", "glibenclamida", "gliclazida de liberação modificada",
            "glimepirida", "cloridrato de pioglitazona", "fosfato de sitagliptina", "vildagliptina", "linagliptina", "propanodiol de dapagliflozina",
            "empagliflozina", "insulina humana", "insulina humana nph", "insulina humana regular")
        add("Colesterol", "sinvastatina", "atorvastatina cálcica", "rosuvastatina cálcica", "pravastatina sódica", "ezetimiba", "fenofibrato", "genfibrozila")
        add("Tireoide", "levotiroxina sódica", "metimazol", "propiltiouracila")
        add("Estômago", "omeprazol", "pantoprazol sódico", "esomeprazol magnésico", "lansoprazol", "domperidona", "sucralfato", "cloridrato de metoclopramida")
        add("Vitaminas e minerais", "colecalciferol", "ácido fólico", "cianocobalamina", "carbonato de cálcio + colecalciferol", "sulfato ferroso", "cloreto de potássio")
        add("Coração e circulação", "ácido acetilsalicílico", "bissulfato de clopidogrel", "varfarina sódica", "mononitrato de isossorbida", "cloridrato de amiodarona")
    }

    fun de(principioAtivo: String): String = porPrincipio[principioAtivo.lowercase()] ?: "Outros cuidados"
}
