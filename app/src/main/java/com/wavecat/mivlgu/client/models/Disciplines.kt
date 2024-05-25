package com.wavecat.mivlgu.client.models

typealias Disciplines = List<Para>
typealias DisciplinesByParity = Map<String, Disciplines>
typealias DisciplineDuringDay = Map<String, DisciplinesByParity>
typealias DisciplinesForWeek = Map<String, DisciplineDuringDay>
