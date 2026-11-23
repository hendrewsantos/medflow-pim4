using MedFlow.Domain.Entidades;
using MedFlow.Domain.Enums;

namespace MedFlow.Infrastructure.Persistencia;

/// <summary>Catálogo ampliado de demonstração: medicamentos de uso contínuo mais comuns, com lote único de validade longa.
/// Flags: ce = controle especial, am = antimicrobiano, tl = termolábil (esses três ficam fora da Box, RN09).</summary>
internal static class CatalogoAdicional
{
    private static readonly (string Nome, string Principio, string Concentracao, string Forma, Tarja Tarja, int Minimo, string Flag, int Quantidade)[] Itens =
    {
        ("Enalapril", "maleato de enalapril", "10 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Captopril", "captopril", "25 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Ramipril", "ramipril", "5 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Lisinopril", "lisinopril", "10 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Valsartana", "valsartana", "80 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Olmesartana", "olmesartana medoxomila", "20 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Candesartana", "candesartana cilexetila", "16 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Carvedilol", "carvedilol", "6,25 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Metoprolol", "succinato de metoprolol", "50 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Propranolol", "cloridrato de propranolol", "40 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Bisoprolol", "fumarato de bisoprolol", "5 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Espironolactona", "espironolactona", "25 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Clortalidona", "clortalidona", "25 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Indapamida", "indapamida", "1,5 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Furosemida", "furosemida", "40 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Hidralazina", "cloridrato de hidralazina", "25 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Metformina XR", "cloridrato de metformina de liberação prolongada", "500 mg", "comprimido", Tarja.Vermelha, 80, "", 240),
        ("Glibenclamida", "glibenclamida", "5 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Gliclazida", "gliclazida de liberação modificada", "60 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Glimepirida", "glimepirida", "2 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Pioglitazona", "cloridrato de pioglitazona", "30 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Sitagliptina", "fosfato de sitagliptina", "100 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Vildagliptina", "vildagliptina", "50 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Linagliptina", "linagliptina", "5 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Dapagliflozina", "propanodiol de dapagliflozina", "10 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Empagliflozina", "empagliflozina", "25 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Atorvastatina", "atorvastatina cálcica", "20 mg", "comprimido", Tarja.Vermelha, 80, "", 240),
        ("Rosuvastatina", "rosuvastatina cálcica", "10 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Pravastatina", "pravastatina sódica", "20 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Ezetimiba", "ezetimiba", "10 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Fenofibrato", "fenofibrato", "200 mg", "cápsula", Tarja.Vermelha, 30, "", 120),
        ("Genfibrozila", "genfibrozila", "600 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Levotiroxina sódica", "levotiroxina sódica", "100 mcg", "comprimido", Tarja.Vermelha, 80, "", 240),
        ("Levotiroxina sódica", "levotiroxina sódica", "25 mcg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Levotiroxina sódica", "levotiroxina sódica", "75 mcg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Metimazol", "metimazol", "5 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Propiltiouracila", "propiltiouracila", "100 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Pantoprazol", "pantoprazol sódico", "40 mg", "comprimido", Tarja.Vermelha, 60, "", 180),
        ("Esomeprazol", "esomeprazol magnésico", "40 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Lansoprazol", "lansoprazol", "30 mg", "cápsula", Tarja.Vermelha, 40, "", 120),
        ("Domperidona", "domperidona", "10 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Sucralfato", "sucralfato", "1 g", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Metoclopramida", "cloridrato de metoclopramida", "10 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Colecalciferol (vitamina D3)", "colecalciferol", "2.000 UI", "cápsula", Tarja.Livre, 80, "", 240),
        ("Ácido fólico", "ácido fólico", "5 mg", "comprimido", Tarja.Livre, 60, "", 180),
        ("Cianocobalamina (vitamina B12)", "cianocobalamina", "1.000 mcg", "comprimido", Tarja.Livre, 40, "", 120),
        ("Carbonato de cálcio com vitamina D", "carbonato de cálcio + colecalciferol", "500 mg + 400 UI", "comprimido", Tarja.Livre, 60, "", 180),
        ("Sulfato ferroso", "sulfato ferroso", "40 mg de ferro", "comprimido", Tarja.Livre, 60, "", 180),
        ("Cloreto de potássio", "cloreto de potássio", "600 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Clopidogrel", "bissulfato de clopidogrel", "75 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Varfarina", "varfarina sódica", "5 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Isossorbida", "mononitrato de isossorbida", "20 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Amiodarona", "cloridrato de amiodarona", "200 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Alopurinol", "alopurinol", "100 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Finasterida", "finasterida", "5 mg", "comprimido", Tarja.Vermelha, 40, "", 120),
        ("Tansulosina", "cloridrato de tansulosina", "0,4 mg", "cápsula", Tarja.Vermelha, 40, "", 120),
        ("Loratadina", "loratadina", "10 mg", "comprimido", Tarja.Livre, 40, "", 120),
        ("Montelucaste", "montelucaste de sódio", "10 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Gabapentina", "gabapentina", "300 mg", "cápsula", Tarja.Vermelha, 40, "", 120),
        ("Carbamazepina", "carbamazepina", "200 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Donepezila", "cloridrato de donepezila", "5 mg", "comprimido", Tarja.Vermelha, 30, "", 120),
        ("Paracetamol", "paracetamol", "750 mg", "comprimido", Tarja.Livre, 60, "", 180),
        ("Dipirona", "dipirona sódica", "500 mg", "comprimido", Tarja.Livre, 60, "", 180),
        ("Sertralina", "cloridrato de sertralina", "50 mg", "comprimido", Tarja.Vermelha, 30, "ce", 120),
        ("Fluoxetina", "cloridrato de fluoxetina", "20 mg", "cápsula", Tarja.Vermelha, 30, "ce", 120),
        ("Zolpidem", "hemitartarato de zolpidem", "10 mg", "comprimido", Tarja.Preta, 20, "ce", 120),
        ("Azitromicina", "azitromicina di-hidratada", "500 mg", "comprimido", Tarja.Vermelha, 30, "am", 120),
        ("Cefalexina", "cefalexina monoidratada", "500 mg", "cápsula", Tarja.Vermelha, 30, "am", 120),
        ("Insulina regular", "insulina humana regular", "100 UI/mL", "frasco-ampola", Tarja.Vermelha, 15, "tl", 120),
    };

    private static readonly Dictionary<string, decimal> Precos = new(StringComparer.OrdinalIgnoreCase)
    {
        ["losartana potássica"] = 0.32m,
        ["cloridrato de metformina"] = 0.28m,
        ["sinvastatina"] = 0.35m,
        ["ácido acetilsalicílico"] = 0.12m,
        ["omeprazol"] = 0.45m,
        ["levotiroxina sódica"] = 0.60m,
        ["atenolol"] = 0.25m,
        ["hidroclorotiazida"] = 0.20m,
        ["besilato de anlodipino"] = 0.30m,
        ["clonazepam"] = 1.20m,
        ["amoxicilina"] = 1.10m,
        ["insulina humana"] = 62.00m,
        ["amoxicilina tri-hidratada"] = 1.10m,
        ["insulina humana nph"] = 62.00m,
        ["maleato de enalapril"] = 0.22m,
        ["captopril"] = 0.18m,
        ["ramipril"] = 0.55m,
        ["lisinopril"] = 0.40m,
        ["valsartana"] = 0.90m,
        ["olmesartana medoxomila"] = 1.20m,
        ["candesartana cilexetila"] = 1.10m,
        ["carvedilol"] = 0.50m,
        ["succinato de metoprolol"] = 0.65m,
        ["cloridrato de propranolol"] = 0.20m,
        ["fumarato de bisoprolol"] = 0.70m,
        ["espironolactona"] = 0.45m,
        ["clortalidona"] = 0.40m,
        ["indapamida"] = 0.55m,
        ["furosemida"] = 0.15m,
        ["cloridrato de hidralazina"] = 0.60m,
        ["cloridrato de metformina de liberação prolongada"] = 0.45m,
        ["glibenclamida"] = 0.18m,
        ["gliclazida de liberação modificada"] = 0.85m,
        ["glimepirida"] = 0.50m,
        ["cloridrato de pioglitazona"] = 0.95m,
        ["fosfato de sitagliptina"] = 4.50m,
        ["vildagliptina"] = 4.20m,
        ["linagliptina"] = 6.00m,
        ["propanodiol de dapagliflozina"] = 5.20m,
        ["empagliflozina"] = 6.50m,
        ["atorvastatina cálcica"] = 0.90m,
        ["rosuvastatina cálcica"] = 1.10m,
        ["pravastatina sódica"] = 0.60m,
        ["ezetimiba"] = 1.40m,
        ["fenofibrato"] = 1.20m,
        ["genfibrozila"] = 0.80m,
        ["metimazol"] = 0.50m,
        ["propiltiouracila"] = 0.70m,
        ["pantoprazol sódico"] = 0.70m,
        ["esomeprazol magnésico"] = 1.30m,
        ["lansoprazol"] = 0.95m,
        ["domperidona"] = 0.40m,
        ["sucralfato"] = 0.90m,
        ["cloridrato de metoclopramida"] = 0.15m,
        ["colecalciferol"] = 0.90m,
        ["ácido fólico"] = 0.20m,
        ["cianocobalamina"] = 0.50m,
        ["carbonato de cálcio + colecalciferol"] = 0.70m,
        ["sulfato ferroso"] = 0.25m,
        ["cloreto de potássio"] = 0.55m,
        ["bissulfato de clopidogrel"] = 1.40m,
        ["varfarina sódica"] = 0.45m,
        ["mononitrato de isossorbida"] = 0.50m,
        ["cloridrato de amiodarona"] = 0.65m,
        ["alopurinol"] = 0.20m,
        ["finasterida"] = 0.60m,
        ["cloridrato de tansulosina"] = 0.80m,
        ["loratadina"] = 0.30m,
        ["montelucaste de sódio"] = 1.50m,
        ["gabapentina"] = 0.75m,
        ["carbamazepina"] = 0.30m,
        ["cloridrato de donepezila"] = 1.60m,
        ["paracetamol"] = 0.25m,
        ["dipirona sódica"] = 0.20m,
        ["cloridrato de sertralina"] = 0.90m,
        ["cloridrato de fluoxetina"] = 0.45m,
        ["hemitartarato de zolpidem"] = 1.80m,
        ["azitromicina di-hidratada"] = 3.20m,
        ["cefalexina monoidratada"] = 1.60m,
        ["insulina humana regular"] = 64.00m,
    };

    /// <summary>Preço fictício por unidade (R$) para o pagamento simulado.</summary>
    public static decimal PrecoDe(string principioAtivo) => Precos.TryGetValue(principioAtivo, out var p) ? p : 0.60m;

    public static List<Medicamento> Criar(DateTime hoje, DateTime agora)
    {
        var lista = new List<Medicamento>();
        var seq = 0;
        foreach (var i in Itens)
        {
            var controle = i.Flag == "ce";
            var categoria = i.Flag == "am" ? CategoriaRegulatoria.Antimicrobiano : CategoriaRegulatoria.Comum;
            var m = new Medicamento(i.Nome, i.Principio, i.Concentracao, i.Forma, i.Tarja, controle, i.Minimo, categoria, termolabil: i.Flag == "tl");
            seq++;
            m.RegistrarEntrada($"CAT{seq:000}", hoje.AddMonths(-2), hoje.AddMonths(12), i.Quantidade, agora);
            m.DefinirPreco(PrecoDe(i.Principio));
            lista.Add(m);
        }
        return lista;
    }
}
