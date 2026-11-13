package example.bairrovivo.saude;

import java.util.List;

/** Health triage at the neighbourhood clinic (posto de saúde). */
public class Triagem {

    private String cns;

    private String cid10;

    private List<String> sintomas;

    private Integer prioridade;

    public String getCns() {
        return cns;
    }

    public String getCid10() {
        return cid10;
    }

    public List<String> getSintomas() {
        return sintomas;
    }

    public Integer getPrioridade() {
        return prioridade;
    }
}
