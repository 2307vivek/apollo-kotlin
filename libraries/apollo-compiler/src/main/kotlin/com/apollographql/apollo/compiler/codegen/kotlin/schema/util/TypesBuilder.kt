package com.apollographql.apollo.compiler.codegen.kotlin.schema.util

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.newBuilder
import com.apollographql.apollo.compiler.codegen.Identifier.type
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinResolver
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.toListInitializerCodeblock
import com.apollographql.apollo.compiler.ir.IrEnum
import com.apollographql.apollo.compiler.ir.IrInterface
import com.apollographql.apollo.compiler.ir.IrObject
import com.apollographql.apollo.compiler.ir.IrScalar
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal fun IrScalar.typePropertySpec(target: TypeName): PropertySpec {
  return PropertySpec
      .builder(Identifier.type, KotlinSymbols.CustomScalarType)
      .initializer("%T(%S, %S)", KotlinSymbols.CustomScalarType, name, target.toString())
      .build()
}

internal fun IrEnum.typePropertySpec(): PropertySpec {
  return PropertySpec
      .builder(Identifier.type, KotlinSymbols.EnumType)
      .initializer("%T(%S, %L)", KotlinSymbols.EnumType, name, this.values.map { CodeBlock.of("%S", it.name) }.toListInitializerCodeblock())
      .build()
}

private fun List<String>.toCode(): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add("listOf(")
  builder.add("%L", sorted().map { CodeBlock.of("%S", it) }.joinToCode(", "))
  builder.add(")")
  return builder.build()
}

private fun List<String>.implementsToCode(resolver: KotlinResolver): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add("listOf(")
  builder.add("%L", sorted().map {
    resolver.resolveCompiledType(it)
  }.joinToCode(", "))
  builder.add(")")
  return builder.build()
}

internal fun IrObject.typePropertySpec(resolver: KotlinResolver): PropertySpec {
  val builder = CodeBlock.builder()
  builder.add("%T(name = %S)", KotlinSymbols.ObjectTypeBuilder, name)
  if (keyFields.isNotEmpty()) {
    builder.add(".keyFields(%L)", keyFields.toCode())
  }
  if (implements.isNotEmpty()) {
    builder.add(".interfaces(%L)", implements.implementsToCode(resolver))
  }
  if (embeddedFields.isNotEmpty()) {
    builder.add(".embeddedFields(%L)", embeddedFields.toCode())
  }
  builder.add(".build()")

  return PropertySpec
      .builder(type, KotlinSymbols.ObjectType)
      .initializer(builder.build())
      .build()
}

internal fun IrInterface.typePropertySpec(resolver: KotlinResolver): PropertySpec {
  val builder = CodeBlock.builder()
  builder.add("%T(name = %S)", KotlinSymbols.InterfaceTypeBuilder, name)
  if (keyFields.isNotEmpty()) {
    builder.add(".keyFields(%L)", keyFields.toCode())
  }
  if (implements.isNotEmpty()) {
    builder.add(".interfaces(%L)", implements.implementsToCode(resolver))
  }
  if (embeddedFields.isNotEmpty()) {
    builder.add(".embeddedFields(%L)", embeddedFields.toCode())
  }
  builder.add(".build()")

  return PropertySpec
      .builder(type, KotlinSymbols.InterfaceType)
      .initializer(builder.build())
      .build()
}


internal fun IrUnion.typePropertySpec(resolver: KotlinResolver): PropertySpec {
  val builder = CodeBlock.builder()
  builder.add(members.map {
    resolver.resolveCompiledType(it)
  }.joinToCode(", "))

  return PropertySpec
      .builder(type, KotlinSymbols.UnionType)
      .maybeAddDescription(description)
      .maybeAddDeprecation(deprecationReason)
      .initializer("%T(%S, %L)", KotlinSymbols.UnionType, name, builder.build())
      .build()
}

internal fun dataTypeSpec(): TypeSpec {
  return TypeSpec.interfaceBuilder(Identifier.Data)
      .addSuperinterface(KotlinSymbols.FragmentData)
      .build()
}
