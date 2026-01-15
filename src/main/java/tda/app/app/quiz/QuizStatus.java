package tda.app.app.quiz;

/**
 * Stav kvízu z pohledu dostupnosti pro studenta.
 */
public enum QuizStatus {
    /** Student může vyplnit a odevzdat pokus. */
    OPEN,
    /** Student nemůže odevzdat nový pokus, ale může zobrazit své výsledky. */
    CLOSED
}
