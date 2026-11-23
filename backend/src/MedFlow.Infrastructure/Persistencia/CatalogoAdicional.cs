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
            lista.Add(m);
        }
        return lista;
    }
}
