package org.example

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.text.Normalizer
import java.util.*
import java.nio.file.StandardOpenOption

enum class Categoria(val sigla: Char){
    ROUPA('R'), ELETRONICO('E'), COLECIONAVEL('C');
}
enum class TipoRoupa{
    CAMISA, MOLETON, ACESSORIO
}
enum class TamanhoRoupa{
    PP, P, M, G, GG, XG, XXG
}

enum class TipoEletronico{
    VIDEO_GAME, JOGO, PORTATIL, OUTROS
}

enum class TipoColecionavel{
    LIVRO, BONECO, OUTROS
}

enum class Material{
    PAPEL, PLASTICO, ACO, MISTURADO, OUTROS
}

enum class Relevancia{
    COMUM, MEDIO, RARO, RARISSIMO
}

open class Produto (
    val nome: String,
    val precoCompra: Float,
    val precoVenda: Float,
    val codigoProduto: String,
    val categoria: Categoria
){
    val codigoModificado: String = "${categoria.sigla}-${codigoProduto}"
}

private data class Estoque(val produto: Produto, var quantidade: Int)

class Roupas(
    nome: String,
    precoCompra: Float,
    precoVenda: Float,
    codigoProduto: String,
    val tipoRoupa: TipoRoupa,
    val tamanhoRoupa: TamanhoRoupa,
    val corPrimaria: String,
    val corSecundaria: String?
) : Produto(nome, precoCompra, precoVenda, codigoProduto, Categoria.ROUPA)

class Eletronicos(
    nome: String,
    precoCompra: Float,
    precoVenda: Float,
    codigoProduto: String,
    val tipoEletronico: TipoEletronico,
    val versao: String,
    val anoFabricacao: Int?
) : Produto(nome, precoCompra, precoVenda, codigoProduto, Categoria.ELETRONICO)

class Colecionavel(
    nome: String,
    precoCompra: Float,
    precoVenda: Float,
    codigoProduto: String,
    val tipoColecionavel: TipoColecionavel,
    val material: Material,
    val tamanho: Float?,
    val relevancia: Relevancia
) : Produto(nome, precoCompra, precoVenda, codigoProduto, Categoria.COLECIONAVEL)

private fun normalizacao(entrada: String?): String {
    if (entrada == null) return "-"
    val aux1 = Normalizer.normalize(entrada, Normalizer.Form.NFD)
    val semAcento = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(aux1, "")
    val res = semAcento.trim().uppercase(Locale.getDefault())
    return res.ifBlank { "-" }
}

private fun opcional(entrada: String): String? =
    normalizacao(entrada).let { if (it == "-") null else it }

private data class CompraCsv(
    val nome: String,
    val precoCompra: Float,
    val precoVenda: Float,
    val codigoProduto: String,
    val quantidade: Int,
    val categoria: String,
    val tipo: String,
    val tamanho: String,
    val corPrimaria: String,
    val corSecundaria: String,
    val versao: String,
    val anoFabricacao: String,
    val materialFabricacao: String,
    val relevancia: String
)
private data class VendaCsv(val codigoModificado: String, val quantidade: Int)

private fun caminhoCompra(entrada: Path): Path{
    val arquivo = entrada.resolve("compras.csv")
    require(Files.exists(arquivo)) { "Não foi encontrado o arquivo de entrada compras.csv"}
    return arquivo
}
private fun caminhoVendas(pastaEntrada: Path): Path {
    val arquivo = pastaEntrada.resolve("vendas.csv")
    require(Files.exists(arquivo)) { "Não foi encontrado o arquivo de entrada vendas.csv" }
    return arquivo
}

private fun leCsvCompra(arquivo: File): List<CompraCsv> {
    require(arquivo.exists()) { "Não foi encontrado compras.csv" }
    return arquivo.useLines { seq ->
        seq.drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val c = line.split(",")
                CompraCsv(
                    codigoProduto = normalizacao(c[0]),
                    quantidade   = c[1].toInt(),
                    nome         = normalizacao(c[2]),
                    precoCompra  = c[3].toFloat(),
                    precoVenda   = c[4].toFloat(),
                    categoria    = normalizacao(c[5]),
                    tipo         = c[6],
                    tamanho      = c[7],
                    corPrimaria  = c[8],
                    corSecundaria= c[9],
                    versao       = c[10],
                    anoFabricacao= c[11],
                    materialFabricacao = c[12],
                    relevancia   = c[13]
                )
            }.toList()
    }
}

private fun lerVendas(arquivo: File): List<VendaCsv> {
    require(arquivo.exists()) { "Não foi encontrado vendas.csv" }
    return arquivo.useLines { seq ->
        seq.drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val v = line.split(",")
                VendaCsv(
                    codigoModificado = normalizacao(v[0]),
                    quantidade = v[1].toInt()
                )
            }.toList()
    }
}

private fun atribuirProduto(c: CompraCsv): Produto {
    return when (c.categoria) {
        "ROUPA" -> {
            val tipoRoupa = when (normalizacao(c.tipo)) {
                "CAMISA" -> TipoRoupa.CAMISA
                "MOLETON", "MOLETOM" -> TipoRoupa.MOLETON
                else -> TipoRoupa.ACESSORIO
            }
            val tam = when (normalizacao(c.tamanho)) {
                "PP"->TamanhoRoupa.PP; "P"->TamanhoRoupa.P; "M"->TamanhoRoupa.M; "G"->TamanhoRoupa.G
                "GG"->TamanhoRoupa.GG; "XG"->TamanhoRoupa.XG; "XXG"->TamanhoRoupa.XXG
                else -> TamanhoRoupa.M
            }
            Roupas(
                nome = c.nome,
                precoCompra = c.precoCompra,
                precoVenda = c.precoVenda,
                codigoProduto = c.codigoProduto,
                tipoRoupa = tipoRoupa,
                tamanhoRoupa = tam,
                corPrimaria = normalizacao(c.corPrimaria),
                corSecundaria = opcional(c.corSecundaria)
            )
        }
        "ELETRONICO" -> {
            val tipoEle = when (normalizacao(c.tipo)) {
                "VIDEO-GAME", "VIDEO GAME", "VIDEOGAME" -> TipoEletronico.VIDEO_GAME
                "JOGO" -> TipoEletronico.JOGO
                "PORTATIL" -> TipoEletronico.PORTATIL
                else -> TipoEletronico.OUTROS
            }
            Eletronicos(
                nome = c.nome,
                precoCompra = c.precoCompra,
                precoVenda = c.precoVenda,
                codigoProduto = c.codigoProduto,
                tipoEletronico = tipoEle,
                versao = opcional(c.versao) ?: "-",
                anoFabricacao = opcional(c.anoFabricacao)?.toIntOrNull()
            )
        }
        "COLECIONAVEL" -> {
            val tipoCol = when (normalizacao(c.tipo)) {
                "LIVRO" -> TipoColecionavel.LIVRO
                "BONECO" -> TipoColecionavel.BONECO
                else -> TipoColecionavel.OUTROS
            }
            val mat = when (normalizacao(c.materialFabricacao)) {
                "PAPEL"->Material.PAPEL; "PLASTICO"->Material.PLASTICO; "ACO"->Material.ACO
                "MISTURADO"->Material.MISTURADO
                else -> Material.OUTROS
            }
            val rel = when (normalizacao(c.relevancia)) {
                "COMUM"->Relevancia.COMUM; "MEDIO"->Relevancia.MEDIO
                "RARO"->Relevancia.RARO; "RARISSIMO"->Relevancia.RARISSIMO
                else -> Relevancia.COMUM
            }
            Colecionavel(
                nome = c.nome,
                precoCompra = c.precoCompra,
                precoVenda = c.precoVenda,
                codigoProduto = c.codigoProduto,
                tipoColecionavel = tipoCol,
                material = mat,
                tamanho = opcional(c.tamanho)?.toFloatOrNull(),
                relevancia = rel
            )
        }
        else -> error("Categoria inválida: ${c.categoria}")
    }
}

private data class Balanco(var totalCompras: Double = 0.0, var totalVendas: Double = 0.0)

private fun processar(compras: List<CompraCsv>, vendas: List<VendaCsv>): Pair<Map<String, Estoque>, Balanco> {
    val estoque = LinkedHashMap<String, Estoque>()
    val bal = Balanco()

    for (c in compras) {
        val p = atribuirProduto(c)
        val key = p.codigoModificado
        val e = estoque[key]
        if (e == null) {
            estoque[key] = Estoque(p, c.quantidade)
        } else {
            e.quantidade += c.quantidade
        }
        bal.totalCompras += c.quantidade * p.precoCompra
    }

    for (v in vendas) {
        val e = estoque[v.codigoModificado]
        if (e != null) {
            e.quantidade -= v.quantidade
            bal.totalVendas += v.quantidade * e.produto.precoVenda
        }
    }

    return estoque to bal
}

private fun escreverEstoqueGeral(saida: Path, estoque: Map<String, Estoque>) {
    val destino = saida.resolve("estoque_geral.csv")
    val linhas = buildString {
        appendLine("CODIGO,NOME,QUANTIDADE")
        estoque.forEach { (codigoMod, e) ->
            val nome = e.produto.nome
            appendLine("$codigoMod,$nome,${e.quantidade}")
        }
    }

    destino.toFile().writeText(linhas)

}

private fun escreverEstoquePorCategoria(saida: Path, estoque: Map<String, Estoque>) {
    val destino = saida.resolve("estoque_categoria.csv")
    val porCategoria = linkedMapOf<Categoria, Int>()
    estoque.values.forEach { e ->
        porCategoria.merge(e.produto.categoria, e.quantidade) { a, b -> a + b }
    }
    val linhas = buildString {
        appendLine("CATEGORIA,QUANTIDADE")
        porCategoria.forEach { (cat, qtd) -> appendLine("${cat.name},$qtd") }
    }
    destino.toFile().writeText(linhas)
}

private fun escreverBalancete(outDir: Path, balanco: Balanco) {
    val destino = outDir.resolve("balancete.csv")
    val diferenca = balanco.totalVendas - balanco.totalCompras
    fun formata(x: Double): String = String.format(Locale.US, "%.2f", x).trimEnd('0').trimEnd('.')
    val linhas = buildString {
        appendLine("COMPRAS,${formata(balanco.totalCompras)}")
        appendLine("VENDAS,${formata(balanco.totalVendas)}")
        appendLine("BALANCETE,${formata(diferenca)}")
    }
    destino.toFile().writeText(linhas)
}

private fun executarBusca(entrada: Path, saida: Path, estoque: Map<String, Estoque>) {
    val buscaCsv = entrada.resolve("busca.csv").toFile()
    if (!buscaCsv.exists()) return

    val sb = StringBuilder().apply { appendLine("BUSCAS,QUANTIDADE") }
    var idx = 0

    buscaCsv.useLines { lines ->
        lines.drop(1).forEach { line ->
            idx++
            val s = line.split(",")

            val categoria       = normalizacao(s.getOrNull(0) ?: "-")
            val tipo           = normalizacao(s.getOrNull(1) ?: "-")
            val tamanho           = normalizacao(s.getOrNull(2) ?: "-")
            val corPrimaria   = normalizacao(s.getOrNull(3) ?: "-")
            val corSecundaria = normalizacao(s.getOrNull(4) ?: "-")
            val versao        = normalizacao(s.getOrNull(5) ?: "-")
            val anoFabricacaoStr        = normalizacao(s.getOrNull(6) ?: "-")
            val material       = normalizacao(s.getOrNull(7) ?: "-")
            val relevancia      = normalizacao(s.getOrNull(8) ?: "-")
            val anoFabricacao        = if (anoFabricacaoStr == "-") null else anoFabricacaoStr.toIntOrNull()

            var qtd = 0
            for ((_, e) in estoque) {
                val p = e.produto

                val catOk =
                    categoria == "-" ||
                            (categoria == "ROUPA"        && p.categoria == Categoria.ROUPA) ||
                            (categoria == "ELETRONICO"   && p.categoria == Categoria.ELETRONICO) ||
                            (categoria == "COLECIONAVEL" && p.categoria == Categoria.COLECIONAVEL)
                if (!catOk) continue

                when (p) {
                    is Roupas -> {
                        if (!(tipo == "-" || p.tipoRoupa.name == tipo)) continue
                        if (!(tamanho == "-" || p.tamanhoRoupa.name == tamanho)) continue
                        if (!(corPrimaria == "-" || normalizacao(p.corPrimaria) == corPrimaria)) continue
                        val sec = p.corSecundaria?.let(::normalizacao) ?: "-"
                        if (!(corSecundaria == "-" || sec == corSecundaria)) continue
                    }
                    is Eletronicos -> {
                        if (!(tipo == "-" || p.tipoEletronico.name == tipo)) continue
                        if (!(versao == "-" || normalizacao(p.versao) == versao)) continue
                        if (!(anoFabricacao == null || p.anoFabricacao == anoFabricacao)) continue
                    }
                    is Colecionavel -> {
                        if (!(tipo == "-" || p.tipoColecionavel.name == tipo)) continue
                        if (!(material == "-" || p.material.name == material)) continue
                        if (!(relevancia == "-" || p.relevancia.name == relevancia)) continue
                    }
                }
                qtd += e.quantidade
            }

            sb.append(idx).append(',').append(qtd).append('\n')
        }
    }

    saida.resolve("resultado_busca.csv").toFile().writeText(sb.toString())

}

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Entrada incorreta, esperado <pasta_entrada> <pasta_saida>")
        return
    }
    val entrada = Path.of(args[0])
    val saida   = Path.of(args[1])

    require(Files.exists(entrada)) { "Nao encontrada a pasta de entrada" }
    if (Files.notExists(saida)) {
        Files.createDirectories(saida)}

    val compras = leCsvCompra(caminhoCompra(entrada).toFile())
    val vendas  = lerVendas(caminhoVendas(entrada).toFile())

    val (estoque, balanco) = processar(compras, vendas)

    escreverEstoqueGeral(saida, estoque)
    escreverEstoquePorCategoria(saida, estoque)
    escreverBalancete(saida, balanco)

    executarBusca(entrada, saida, estoque)
}
