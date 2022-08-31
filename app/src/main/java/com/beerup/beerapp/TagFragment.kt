package com.beerup.beerapp

import com.beerup.beerapp.BeerDetailsFragment
import com.beerup.beerapp.R
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.beerup.beerapp.ViewModels.TagViewModel


class TagFragment(var modifyTag: (String, Boolean) -> Unit) : Fragment() {


    lateinit var tagText: TextView
    lateinit var tagLayout: LinearLayout
    private lateinit var viewModel: TagViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var tagName: String? = null
        var added: Boolean? = null
        if (arguments != null) {
            tagName = arguments?.getString("tagName")
            added  = arguments?.getBoolean("added")
        }

        viewModel = ViewModelProvider(this).get(TagViewModel::class.java)
        viewModel.tagName = tagName!!
        viewModel.added = added!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.tag_layout, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tagText = view.findViewById(R.id.tagtext)
        tagLayout = view.findViewById(R.id.taglayout)
        // if i dont save paddings they will change when i change background cause android :)
        val paddingBottom: Int = tagLayout.paddingBottom
        val paddingLeft: Int = tagLayout.paddingLeft
        val paddingRight: Int = tagLayout.paddingRight
        val paddingTop: Int = tagLayout.paddingTop

        if(viewModel.added) {
            viewModel.add = true
            tagLayout.background = activity?.getDrawable(R.drawable.tagitemadded)
            tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

            if(activity?.resources!!.configuration.uiMode and // i need to check if this first part is important
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                tagText.setTextColor(activity?.resources!!.getColor(R.color.white))
            } else {
                tagText.setTextColor(activity?.resources!!.getColor(R.color.black))
            }
        } else {
            if (activity?.resources!!.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            ) {
                tagLayout.background = activity?.getDrawable(R.drawable.tagitemnotaddednight)
                tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            }
        }

        tagLayout.setOnClickListener{

            viewModel.add = !viewModel.add // so cool that kotlin dont have toggle but have empty and Notempty methods on string :)
            modifyTag(viewModel.tagName, viewModel.add)

            if(viewModel.add){
                tagLayout.background = activity?.getDrawable(R.drawable.tagitemadded)
                tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

                if(activity?.resources!!.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    tagText.setTextColor(activity?.resources!!.getColor(R.color.white))
                } else {
                    tagText.setTextColor(activity?.resources!!.getColor(R.color.black))
                }
            } else {
                if(activity?.resources!!.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    tagLayout.background = activity?.getDrawable(R.drawable.tagitemnotaddednight)
                } else {
                    tagLayout.background = activity?.getDrawable(R.drawable.tagitemnotadded)
                }
                tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
                tagText.setTextColor(activity?.resources!!.getColor(R.color.lightgray))
            }
        }

        tagText.text = viewModel.getTagText()
    }


}