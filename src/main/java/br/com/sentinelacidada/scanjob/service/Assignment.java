package br.com.sentinelacidada.scanjob.service;

/** Work handed to the scanner runner: a scan ({@code type: scan}) or an access check ({@code type: access-check}). */
public sealed interface Assignment permits JobAssignment, AccessCheckAssignment {
}
