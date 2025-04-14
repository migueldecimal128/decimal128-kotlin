package com.decimal128

import java.math.RoundingMode

typealias StatusFlags = Int
const val SUCCESS:              StatusFlags = 0
const val Conversion_syntax:    StatusFlags = 0x00000001
const val Division_by_zero:     StatusFlags = 0x00000002
const val Division_impossible:  StatusFlags = 0x00000004
const val Division_undefined:   StatusFlags = 0x00000008
const val Insufficient_storage: StatusFlags = 0x00000010 /* [when malloc fails]  */
const val Inexact:              StatusFlags = 0x00000020
const val Invalid_context:      StatusFlags = 0x00000040
const val Invalid_operation:    StatusFlags = 0x00000080
const val Lost_digits:          StatusFlags = 0x00000100
const val Overflow:             StatusFlags = 0x00000200
const val Clamped:              StatusFlags = 0x00000400
const val Rounded:              StatusFlags = 0x00000800
const val Subnormal:            StatusFlags = 0x00001000
const val Underflow:            StatusFlags = 0x00002000

/* Name strings for the exceptional conditions                      */
val statusConditionStrings = arrayOf(
    "Conversion syntax", "Division by zero", "Division impossible", "Division undefined",
    "Insufficient storage", "Inexact", "Invalid context", "Invalid operation", "Lost digits",
    "Overflow", "Clamped", "Rounded", "Subnormal", "Underflow")

const val Condition_ZE = "No status"
const val Condition_MultipleFlags = "Multiple status"
const val Condition_InvalidStatus = "Invalid status"

fun getStatusConditionString(status: Int) : String {
    if (status == 0)
        return Condition_ZE
    if ((status and ((Underflow shl 1) - 1)) != 0)
        return Condition_InvalidStatus
    var i = 0
    var t = status;
    while ((t and 1) == 0) {
        ++i;
        t = t ushr 1
    }
    if ((t ushr 1) != 0)
        return Condition_MultipleFlags
    return statusConditionStrings[i];
}

/* IEEE 754 groupings for the flags                                 */
/* [DEC_Clamped, DEC_Lost_digits, DEC_Rounded, and DEC_Subnormal    */
/* are not in IEEE 754]                                             */
val IEEE_754_Division_by_zero  = (Division_by_zero)
val IEEE_754_Inexact           = (Inexact or Lost_digits)
val IEEE_754_Invalid_operation =  (Conversion_syntax or Division_impossible or
    Division_undefined or Insufficient_storage or Invalid_context or Invalid_operation)
val IEEE_754_Overflow          = (Overflow)
val IEEE_754_Underflow         = (Underflow)

/* flags which are normally errors (result is qNaN, infinite, or 0) */
val Errors = (Division_by_zero or IEEE_754_Invalid_operation or IEEE_754_Overflow or IEEE_754_Underflow)
/* flags which cause a result to become qNaN                        */
val NaNs   = IEEE_754_Invalid_operation

/* flags which are normally for information only (finite results)   */
val Information = (Clamped or Rounded or Inexact or Lost_digits)

/* IEEE 854 names (for compatibility with older decNumber versions) */
val IEEE_854_Division_by_zero  = IEEE_754_Division_by_zero
val IEEE_854_Inexact           = IEEE_754_Inexact
val IEEE_854_Invalid_operation = IEEE_754_Invalid_operation
val IEEE_854_Overflow          = IEEE_754_Overflow
val IEEE_854_Underflow         = IEEE_754_Underflow

class SIGFPE() : Exception("Decimal Floating Point Exception")

class DecContext {

    val DECSUBSET = true
    val DECEXTFLAG = true

    /* Maxima and Minima for context settings                           */
    val DEC_MAX_DIGITS = 999999999
    val DEC_MIN_DIGITS = 1
    val DEC_MAX_EMAX = 999999999
    val DEC_MIN_EMAX = 0
    val DEC_MAX_EMIN = 0
    val DEC_MIN_EMIN = -999999999
    val DEC_MAX_MATH = 999999 /* max emax, etc., for math funcs. */


    var digits: Int = 34          /* working precision               */
        private set
    var emax: Int = 999           /* maximum positive exponent       */
        private set
    var emin: Int = -999          /* minimum negative exponent       */
        private set
    private var round: RoundingMode = RoundingMode.HALF_EVEN     /* rounding mode                   */
    private var traps: Int = 0            /* trap-enabler flags              */
    private var status: StatusFlags = 0           /* status flags                    */
    var clamp: Boolean = false    /* flag: apply IEEE exponent clamp */
        private set
    private var extended: Boolean = true  /* flag: special-values allowed    */



    /* ------------------------------------------------------------------ */
    /* decContextClearStatus -- clear bits in current status              */
    /*                                                                    */
    /*  context is the context structure to be queried                    */
    /*  mask indicates the bits to be cleared (the status bit that        */
    /*    corresponds to each 1 bit in the mask is cleared)               */
    /*  returns context                                                   */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun clearStatus(mask: Int): DecContext {
        status = status and mask.inv();
        return this
    }

    /* ------------------------------------------------------------------ */
    /* decContextDefault -- initialize a context structure                */
    /*                                                                    */
    /*  context is the structure to be initialized                        */
    /*  kind selects the required set of default values, one of:          */
    /*      DEC_INIT_BASE       -- select ANSI X3-274 defaults            */
    /*      DEC_INIT_DECIMAL32  -- select IEEE 754 defaults, 32-bit       */
    /*      DEC_INIT_DECIMAL64  -- select IEEE 754 defaults, 64-bit       */
    /*      DEC_INIT_DECIMAL128 -- select IEEE 754 defaults, 128-bit      */
    /*      For any other value a valid context is returned, but with     */
    /*      Invalid_operation set in the status field.                    */
    /*  returns a context structure with the appropriate initial values.  */
    /* ------------------------------------------------------------------ */
    enum class Kind { BASE, DECIMAL32, DECIMAL64, DECIMAL128 }

    fun default(kind: Kind): DecContext {
        round = RoundingMode.HALF_EVEN    // 0.5 to nearest even
        traps = 0                      // no traps set
        status = 0                         // cleared
        clamp = true                      // clamp exponents
        extended = true                   // set
        when (kind) {
            Kind.BASE -> {
                digits = 9                         // 9 digits
                emax = DEC_MAX_EMAX                // 9-digit exponents
                emin = DEC_MIN_EMIN                // .. balanced
                round = RoundingMode.HALF_UP          // 0.5 rises
                traps = Errors                 // all but informational
                clamp = false                          // no clamping
                extended = false                       // cleared
            }
            Kind.DECIMAL32 -> {
                digits = 7                     // digits
                emax = 96                      // Emax
                emin = -95                     // Emin
            }
            Kind.DECIMAL64 -> {
                digits = 16                    // digits
                emax = 384                     // Emax
                emin = -383                    // Emin
            }
            Kind.DECIMAL128 -> {
                digits = 34                    // digits
                emax = 6144                    // Emax
                emin = -6143                   // Emin
            }
        }
        return this
    } // decContextDefault

    /* ------------------------------------------------------------------ */
    /* decContextRestoreStatus -- restore bits in current status          */
    /*                                                                    */
    /*  context is the context structure to be updated                    */
    /*  newstatus is the source for the bits to be restored               */
    /*  mask indicates the bits to be restored (the status bit that       */
    /*    corresponds to each 1 bit in the mask is set to the value of    */
    /*    the correspnding bit in newstatus)                              */
    /*  returns context                                                   */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun restoreStatus(newstatus: Int, mask: Int): DecContext {
        status = status and mask.inv()               // clear the selected bits
        status = status or (mask and newstatus)    // or in the new bits
        return this
    } // decContextRestoreStatus

    /* ------------------------------------------------------------------ */
    /* decContextSaveStatus -- save bits in current status                */
    /*                                                                    */
    /*  context is the context structure to be queried                    */
    /*  mask indicates the bits to be saved (the status bits that         */
    /*    correspond to each 1 bit in the mask are saved)                 */
    /*  returns the AND of the mask and the current status                */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun saveStatus(mask: Int): Int {
        return status and mask
    } // decContextSaveStatus

    /* ------------------------------------------------------------------ */
    /* decContextSetStatus -- set status and raise trap if appropriate    */
    /*                                                                    */
    /*  context is the context structure to be updated                    */
    /*  status  is the DEC_ exception code                                */
    /*  returns the context structure                                     */
    /*                                                                    */
    /* Control may never return from this routine, if there is a signal   */
    /* handler and it takes a long jump.                                  */
    /* ------------------------------------------------------------------ */
    fun setStatus(status: Int): DecContext {
        this.status = this.status or status
        if ((status and traps) != 0)
            throw SIGFPE();
        return this
    }

    /* ------------------------------------------------------------------ */
    /* decContextSetStatusQuiet -- set status without trap                */
    /*                                                                    */
    /*  context is the context structure to be updated                    */
    /*  status  is the DEC_ exception code                                */
    /*  returns the context structure                                     */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun setStatusQuiet(status: Int): DecContext {
        this.status = this.status or status
        return this
    }

    /* ------------------------------------------------------------------ */
    /* decContextTestSavedStatus -- test bits in saved status             */
    /*                                                                    */
    /*  oldstatus is the status word to be tested                         */
    /*  mask indicates the bits to be tested (the oldstatus bits that     */
    /*    correspond to each 1 bit in the mask are tested)                */
    /*  returns 1 if any of the tested bits are 1, or 0 otherwise         */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun testSavedStatus(oldstatus:Int, mask:Int): Boolean {
        return (oldstatus and mask)!=0;
    } // decContextTestSavedStatus

    /* ------------------------------------------------------------------ */
    /* decContextTestStatus -- test bits in current status                */
    /*                                                                    */
    /*  context is the context structure to be updated                    */
    /*  mask indicates the bits to be tested (the status bits that        */
    /*    correspond to each 1 bit in the mask are tested)                */
    /*  returns 1 if any of the tested bits are 1, or 0 otherwise         */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun testStatus(mask:Int): Boolean {
        return (status and mask) != 0
    } // decContextTestStatus

    /* ------------------------------------------------------------------ */
    /* decContextZeroStatus -- clear all status bits                      */
    /*                                                                    */
    /*  context is the context structure to be updated                    */
    /*  returns context                                                   */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun zeroStatus(): DecContext {
        status = 0
        return this
    } // decContextZeroStatus

    fun getStatusString() : String {
        return getStatusConditionString(status)
    }

}


