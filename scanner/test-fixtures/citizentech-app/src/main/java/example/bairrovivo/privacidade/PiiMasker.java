package example.bairrovivo.privacidade;

/** Masks personal data before it is logged or shown. */
public final class PiiMasker {

    private PiiMasker() {
    }

    /** Keeps only the middle six digits: ***.123.456-** */
    public static String cpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return "***";
        }
        String digits = cpf.replaceAll("\\D", "");
        return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
    }
}
